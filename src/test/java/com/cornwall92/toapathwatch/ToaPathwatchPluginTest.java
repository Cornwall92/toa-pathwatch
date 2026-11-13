package com.cornwall92.toapathwatch;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Queue;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.ScriptEvent;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Exercises real event subscriptions and delayed lifecycle work without a game client. */
public class ToaPathwatchPluginTest
{
	private static final int LOBBY = 13454;
	private static final int NEXUS = 14160;
	private Client client;
	private WorldView worldView;
	private Widget header;
	private Widget grid;
	private ToaPathwatchPlugin plugin;
	private OverlayManager overlayManager;
	private ToaPathwatchOverlay overlay;
	private Queue<Runnable> callbacks;
	private EventBus events;

	@Before public void setUp()
	{
		client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		overlayManager = mock(OverlayManager.class);
		overlay = mock(ToaPathwatchOverlay.class);
		ToaPathwatchConfig config = mock(ToaPathwatchConfig.class);
		callbacks = new ArrayDeque<>();
		doAnswer(invocation ->
		{
			callbacks.add(invocation.getArgument(0));
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));

		Player player = mock(Player.class);
		worldView = mock(WorldView.class);
		when(worldView.getId()).thenReturn(WorldView.TOPLEVEL);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(worldView);
		when(player.getLocalLocation()).thenReturn(new LocalPoint(12 * 128 + 64, 12 * 128 + 64, WorldView.TOPLEVEL));
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getVarpValue(VarPlayerID.TOA_MYCONTROLLER)).thenReturn(100);
		region(LOBBY);
		header = mock(Widget.class);
		grid = mock(Widget.class);
		when(client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL)).thenReturn(header);
		when(client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT)).thenReturn(grid);

		plugin = new ToaPathwatchPlugin();
		Guice.createInjector(new AbstractModule()
		{
			@Override protected void configure()
			{
				// Providers avoid injecting the internals of mocked RuneLite services.
				bind(Client.class).toProvider(() -> client);
				bind(ClientThread.class).toProvider(() -> clientThread);
				bind(OverlayManager.class).toProvider(() -> overlayManager);
				bind(ToaPathwatchOverlay.class).toProvider(() -> overlay);
				bind(ToaPathwatchConfig.class).toProvider(() -> config);
			}
		}).injectMembers(plugin);
		events = new EventBus(error -> { throw new AssertionError("Event subscriber failed", error); });
		events.register(plugin);
		plugin.startUp();
		drainCallbacks();
	}

	private void region(int region)
	{
		when(worldView.getBaseX()).thenReturn((region >> 8) * 64);
		when(worldView.getBaseY()).thenReturn((region & 255) * 64);
	}

	private void drainCallbacks()
	{
		int attempts = 0;
		while (!callbacks.isEmpty())
		{
			assertTrue("Client-thread callbacks must finish", ++attempts < 20);
			callbacks.remove().run();
		}
	}

	private void tick()
	{
		events.post(new GameTick());
		drainCallbacks();
	}

	private void partyBuild(int role, int count, int points, int wordOne, int wordTwo)
	{
		ScriptEvent script = mock(ScriptEvent.class);
		when(script.getArguments()).thenReturn(new Object[]{6729, role, 12345, count, points, 2, wordOne, wordTwo, 0});
		ScriptPreFired pre = new ScriptPreFired(6729);
		pre.setScriptEvent(script);
		events.post(pre);
		events.post(new ScriptPostFired(6729));
	}

	private void captureWalkThePath(int role)
	{
		partyBuild(role, 1, 50, 0x1000, 0);
		assertEquals("Captured 1 invocations. Raid level: 50.", plugin.getLobbyStatus());
	}

	private void genericButton(int component, String label)
	{
		ScriptEvent script = mock(ScriptEvent.class);
		when(script.getArguments()).thenReturn(new Object[]{1561, 1, component, label,
			InterfaceID.ToaPartydetails.PAUSEBUTTONS, 4792, -1});
		ScriptPreFired pre = new ScriptPreFired(1561);
		pre.setScriptEvent(script);
		events.post(pre);
		events.post(new ScriptPostFired(1561));
	}

	private void enterRaid(int points)
	{
		region(NEXUS);
		when(client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL)).thenReturn(points);
		tick();
	}

	private void changeController(int controller)
	{
		when(client.getVarpValue(VarPlayerID.TOA_MYCONTROLLER)).thenReturn(controller);
		VarbitChanged changed = new VarbitChanged();
		changed.setVarpId(VarPlayerID.TOA_MYCONTROLLER);
		changed.setValue(controller);
		events.post(changed);
	}

	private void gameState(GameState state)
	{
		when(client.getGameState()).thenReturn(state);
		GameStateChanged changed = new GameStateChanged();
		changed.setGameState(state);
		events.post(changed);
	}

	@Test public void startupUsesClientThreadAndShowsReopenInstruction()
	{
		verify(overlayManager).add(overlay);
		assertTrue(callbacks.isEmpty());
		assertNull(plugin.getSelection());
		assertEquals("Open your party's Invocations tab.", plugin.getLobbyStatus());
	}

	@Test public void leaderScriptCapturesEvenWhenWidgetChildrenAreUnavailable()
	{
		assertNull(grid.getChildren());
		captureWalkThePath(2);
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH), plugin.getSelection().active);
		assertNull(plugin.getLobbyStatus());
	}

	@Test public void memberScriptCapturesDespiteWhiteLabelsAndMissingClickHandlers()
	{
		Widget label = mock(Widget.class);
		when(label.getText()).thenReturn("Walk the Path");
		when(label.getTextColor()).thenReturn(0xFFFFFF);
		when(grid.getChildren()).thenReturn(new Widget[]{mock(Widget.class), mock(Widget.class), label});
		captureWalkThePath(1);
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH), plugin.getSelection().active);
	}

	@Test public void controllerEventBeforeFirstRaidTickDoesNotEraseLobbyCapture()
	{
		captureWalkThePath(2);
		// The varp event precedes GameTick, so the old tracker still thought it was in the lobby.
		region(NEXUS);
		changeController(200);
		when(client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL)).thenReturn(50);
		tick();
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH), plugin.getSelection().active);
	}

	@Test public void unreadableThenClosedWidgetsCannotEraseAuthoritativeCapture()
	{
		captureWalkThePath(1);
		when(grid.getChildren()).thenReturn(new Widget[]{null, null, null});
		tick();
		assertEquals("Captured 1 invocations. Raid level: 50.", plugin.getLobbyStatus());
		when(client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL)).thenReturn(null);
		when(client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT)).thenReturn(null);
		tick();
		assertNull(plugin.getLobbyStatus());
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertTrue(plugin.getSelection().active.contains(Invocation.WALK_THE_PATH));
	}

	@Test public void otherPartyAndApplicantScriptsCannotSupplySameScoreSelection()
	{
		for (int role : new int[]{0, 3, 4})
		{
			partyBuild(role, 1, 50, 0x1000, 0);
			assertTrue(plugin.getLobbyStatus().startsWith("Not captured."));
		}
		enterRaid(50);
		assertNull(plugin.getSelection());
	}

	@Test public void viewingAnotherPartyInvalidatesOwnPreviousCapture()
	{
		captureWalkThePath(2);
		partyBuild(0, 1, 50, 0, 0x2000);
		enterRaid(50);
		assertNull(plugin.getSelection());
	}

	@Test public void clickInvalidatesSelectionUntilServerBuildArrives()
	{
		captureWalkThePath(2);
		events.post(new ScriptPreFired(6754));
		tick();
		assertEquals("Waiting for the party's updated selection.", plugin.getLobbyStatus());
		enterRaid(50);
		assertNull(plugin.getSelection());
	}

	@Test public void freshServerBuildAfterClickRestoresCapture()
	{
		captureWalkThePath(2);
		events.post(new ScriptPreFired(6754));
		partyBuild(2, 1, 50, 0, 0x2000);
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.INSANITY), plugin.getSelection().active);
	}

	@Test public void loadingPresetWithoutServerResponseCannotKeepOldSameScoreSelection()
	{
		captureWalkThePath(2);
		genericButton(InterfaceID.ToaPartydetails.BTN_PRESETS_LOAD, "Load");
		tick();
		assertEquals("Waiting for the party's updated selection.", plugin.getLobbyStatus());
		when(header.isHidden()).thenReturn(true);
		when(grid.isHidden()).thenReturn(true);
		enterRaid(50);
		assertNull(plugin.getSelection());
	}

	@Test public void loadingPresetRequiresAndAcceptsFreshServerSelection()
	{
		captureWalkThePath(2);
		genericButton(InterfaceID.ToaPartydetails.BTN_PRESETS_LOAD, "Load");
		partyBuild(2, 1, 50, 0, 0x2000);
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.INSANITY), plugin.getSelection().active);
	}

	@Test public void clearAllInvalidatesUntilConfirmedEmptySelection()
	{
		captureWalkThePath(2);
		genericButton(InterfaceID.ToaPartydetails.INVOCATIONS_CLEAR, "Clear All");
		tick();
		assertEquals("Waiting for the party's updated selection.", plugin.getLobbyStatus());
		partyBuild(2, 0, 0, 0, 0);
		assertEquals("Captured 0 invocations. Raid level: 0.", plugin.getLobbyStatus());
		enterRaid(0);
		assertNotNull(plugin.getSelection());
		assertTrue(plugin.getSelection().active.isEmpty());
	}

	@Test public void unrelatedGenericButtonDoesNotInvalidateCapture()
	{
		captureWalkThePath(2);
		genericButton(InterfaceID.ToaPartydetails.REFRESH, "Refresh");
		tick();
		assertEquals("Captured 1 invocations. Raid level: 50.", plugin.getLobbyStatus());
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH), plugin.getSelection().active);
	}

	@Test public void consecutiveSameScorePresetsUseLatestCompleteScript()
	{
		captureWalkThePath(2);
		// No client-thread callback drain between the two complete pre/post pairs.
		partyBuild(2, 1, 50, 0, 0x2000);
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.INSANITY), plugin.getSelection().active);
	}

	@Test public void controllerChangeWhileStillInLobbyRequiresNewCapture()
	{
		captureWalkThePath(1);
		changeController(201);
		tick();
		assertEquals("Party changed. Reopen your party's Invocations tab.", plugin.getLobbyStatus());
		enterRaid(50);
		assertNull(plugin.getSelection());
	}

	@Test public void logoutClearsCaptureAndPendingWork()
	{
		captureWalkThePath(2);
		gameState(GameState.LOGIN_SCREEN);
		drainCallbacks();
		assertNull(plugin.getLobbyStatus());
		gameState(GameState.LOGGED_IN);
		enterRaid(50);
		assertNull(plugin.getSelection());
	}

	@Test public void confirmedRaidExitCannotReuseSelectionForNextRaid()
	{
		captureWalkThePath(2);
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		region(LOBBY);
		tick();
		assertNull(plugin.getSelection());
		assertEquals("Open your party's Invocations tab for this raid.", plugin.getLobbyStatus());
		enterRaid(50);
		assertNull(plugin.getSelection());
	}

	@Test public void freshLobbyScriptBeforeFirstExitTickBelongsToNextRaid()
	{
		captureWalkThePath(2);
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		region(LOBBY);
		// The next party screen can build before GameTick has observed the raid exit.
		partyBuild(2, 1, 50, 0, 0x2000);
		tick();
		assertEquals("Captured 1 invocations. Raid level: 50.", plugin.getLobbyStatus());
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.INSANITY), plugin.getSelection().active);
	}

	@Test public void partyScriptBeforeStartupCallbackIsNotSilentlyDiscarded()
	{
		plugin.shutDown();
		drainCallbacks();
		plugin.startUp();
		partyBuild(2, 1, 50, 0x1000, 0);
		drainCallbacks();
		assertEquals("Captured 1 invocations. Raid level: 50.", plugin.getLobbyStatus());
		enterRaid(50);
		assertNotNull(plugin.getSelection());
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH), plugin.getSelection().active);
	}

	@Test public void disablingAndEnablingInsideRaidDoesNotInventSelection()
	{
		captureWalkThePath(2);
		enterRaid(50);
		plugin.shutDown();
		plugin.startUp();
		drainCallbacks();
		tick();
		assertNull(plugin.getSelection());
		assertNull(plugin.getLobbyStatus());
		verify(overlayManager).remove(overlay);
	}

	@Test public void pendingStartupCannotRestoreStateAfterShutdown()
	{
		plugin.shutDown();
		drainCallbacks();
		plugin.startUp();
		plugin.shutDown();
		drainCallbacks();
		partyBuild(2, 1, 50, 0x1000, 0);
		enterRaid(50);
		assertNull(plugin.getSelection());
		assertNull(plugin.getLobbyStatus());
	}

	@Test public void scriptWithNoVisiblePartyScreenCannotCapture()
	{
		when(header.isHidden()).thenReturn(true);
		when(grid.isHidden()).thenReturn(true);
		partyBuild(2, 1, 50, 0x1000, 0);
		enterRaid(50);
		assertNull(plugin.getSelection());
	}
}
