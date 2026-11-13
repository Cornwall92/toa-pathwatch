package com.cornwall92.toapathwatch;

import com.google.inject.Provides;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(name = "ToA Pathwatch", description = "Current invocations and path-level reference", tags = {"toa", "invocations", "path", "reference"})
public class ToaPathwatchPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private OverlayManager overlayManager;
	@Inject private ToaPathwatchOverlay overlay;
	@Inject private ToaPathwatchConfig config;

	private volatile boolean enabled;
	private final AtomicInteger generation = new AtomicInteger();
	private int initializedGeneration = -1;
	private boolean partyBuildPending;
	private InvocationSelection pendingSelection;
	private Integer capturedController;
	private String captureStatus = "Open your party's Invocations tab.";
	private boolean showLobbyStatus;
	private final RaidSelectionTracker selections = new RaidSelectionTracker();
	private Boss boss;
	private int pathLevel = -1;

	@Override
	protected void startUp()
	{
		enabled = true;
		int startupGeneration = generation.incrementAndGet();
		overlayManager.add(overlay);
		clientThread.invokeLater(() ->
		{
			if (enabled && generation.get() == startupGeneration)
			{
				initializeState();
				refresh();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		enabled = false;
		int shutdownGeneration = generation.incrementAndGet();
		overlayManager.remove(overlay);
		clientThread.invokeLater(() ->
		{
			if (!enabled && generation.get() == shutdownGeneration) clearState();
		});
	}

	/** Called only on the client thread, including lifecycle cleanup. */
	private void clearState()
	{
		initializedGeneration = generation.get();
		partyBuildPending = false;
		pendingSelection = null;
		capturedController = null;
		captureStatus = "Open your party's Invocations tab.";
		showLobbyStatus = false;
		selections.reset();
		boss = null;
		pathLevel = -1;
	}

	private void initializeState()
	{
		// A game event can arrive before the queued startup callback. Initialize once
		// on the client thread so that callback cannot erase a capture made meanwhile.
		if (initializedGeneration != generation.get()) clearState();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			generation.incrementAndGet();
			clearState();
		}
		else if (event.getGameState() != GameState.LOGGED_IN)
		{
			boss = null;
			pathLevel = -1;
			showLobbyStatus = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) { refresh(); }

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (!enabled) return;
		initializeState();
		if (event.getScriptId() == InvocationReader.PARTY_BUILD_SCRIPT)
		{
			partyBuildPending = event.getScriptEvent() != null;
			pendingSelection = partyBuildPending
				? InvocationReader.readPartyBuild(event.getScriptEvent().getArguments()) : null;
		}
		else if (isSelectionEdit(event) && region() == 13454)
		{
			// A click requests a change; wait for the server's next complete party build.
			// Do not let a partly changed widget grid restore the old selection meanwhile.
			invalidateCapture("Waiting for the party's updated selection.");
		}
	}

	private boolean isSelectionEdit(ScriptPreFired event)
	{
		if (event.getScriptId() == 6754) return true;
		// Script 1558 installs this shared button callback for Load and Clear All.
		// The generic handler also serves unrelated buttons, so check the component.
		if (event.getScriptId() != 1561 || event.getScriptEvent() == null) return false;
		Object[] args = event.getScriptEvent().getArguments();
		return args != null && args.length > 2
			&& (Integer.valueOf(InterfaceID.ToaPartydetails.BTN_PRESETS_LOAD).equals(args[2])
				|| Integer.valueOf(InterfaceID.ToaPartydetails.INVOCATIONS_CLEAR).equals(args[2]));
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (!enabled) return;
		initializeState();
		int id = event.getScriptId();
		if (id == InvocationReader.PARTY_BUILD_SCRIPT && partyBuildPending)
		{
			partyBuildPending = false;
			if (client.getGameState() == GameState.LOGGED_IN && region() == 13454 && partyScreenVisible())
			{
				// The party screen can reopen after raid exit before the next GameTick.
				// Settle that confirmed exit before capture() checks its in-raid guard.
				selections.update(13454, 0);
				if (pendingSelection != null) acceptCapture(pendingSelection);
				else invalidateCapture("Not captured. Reopen your own party's Invocations tab.");
			}
			pendingSelection = null;
		}
	}

	private int region()
	{
		return RegionResolver.region(client);
	}

	private boolean partyScreenVisible()
	{
		var header = client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL);
		var grid = client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT);
		return (header != null && !header.isHidden()) || (grid != null && !grid.isHidden());
	}

	private void acceptCapture(InvocationSelection selection)
	{
		selections.capture(selection);
		capturedController = client.getVarpValue(VarPlayerID.TOA_MYCONTROLLER);
		setCaptureStatus("Captured " + selection.active.size() + " invocations. Raid level: " + selection.raidPoints + ".");
	}

	private void invalidateCapture(String message)
	{
		selections.invalidateLobby();
		capturedController = null;
		setCaptureStatus(message);
	}

	private void setCaptureStatus(String message)
	{
		if (!message.equals(captureStatus)) log.debug("ToA invocation capture: {}", message);
		captureStatus = message;
	}

	private void refresh()
	{
		if (!enabled || client.getGameState() != GameState.LOGGED_IN) return;
		initializeState();
		int region = region();
		// Check party changes against settled geography. A controller update can arrive
		// before the first raid GameTick; clearing from VarbitChanged loses that capture.
		if (region == 13454 && !selections.isInRaid() && capturedController != null
			&& capturedController != client.getVarpValue(VarPlayerID.TOA_MYCONTROLLER))
		{
			invalidateCapture("Party changed. Reopen your party's Invocations tab.");
		}
		boolean wasInRaid = selections.isInRaid();
		// Raid points validate an observed selection; they cannot identify invocations alone.
		selections.update(region, client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL));
		if (wasInRaid && !selections.isInRaid())
		{
			capturedController = null;
			setCaptureStatus("Open your party's Invocations tab for this raid.");
		}
		showLobbyStatus = region == 13454 && partyScreenVisible();
		boss = Boss.forRegion(region, config.showPuzzles());
		pathLevel = -1;
		if (boss != null && boss != Boss.WARDENS)
		{
			Integer hud = InvocationReader.number(client.getWidget(InterfaceID.ToaHud.PATHS_LEVEL));
			// Use the displayed level until the raw varbit encoding is verified in-game.
			if (hud != null && hud >= 0 && hud <= 6) pathLevel = hud;
		}
	}

	Boss getBoss() { return boss; }
	int getPathLevel() { return pathLevel; }
	InvocationSelection getSelection() { return selections.getSelection(); }
	String getLobbyStatus() { return showLobbyStatus ? captureStatus : null; }

	@Provides
	ToaPathwatchConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(ToaPathwatchConfig.class);
	}
}
