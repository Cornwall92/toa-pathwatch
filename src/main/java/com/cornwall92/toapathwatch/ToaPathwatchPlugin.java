package com.cornwall92.toapathwatch;

import com.google.inject.Provides;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

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
	private boolean captureQueued;
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
				clearState();
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
		captureQueued = false;
		selections.reset();
		boss = null;
		pathLevel = -1;
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
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) { refresh(); }

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// Party build, invocation rebuild, and reward-tab toggle. Not RuneLite API constants.
		int id = event.getScriptId();
		if (id == 6617 || id == 6729 || id == 6732) queueCapture();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (!selections.isInRaid() && event.getVarpId() == VarPlayerID.TOA_MYCONTROLLER)
		{
			selections.invalidateLobby();
			queueCapture();
		}
	}

	private int region()
	{
		return RegionResolver.region(client);
	}

	private void captureInvocations()
	{
		if (!enabled || client.getGameState() != GameState.LOGGED_IN || region() != 13454) return;
		InvocationSelection selection = InvocationReader.read(client);
		if (selection != null) selections.capture(selection);
		else
		{
			// An open but unreadable grid may represent changed selections. Discard stale data.
			var widget = client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT);
			if (widget != null && !widget.isHidden()) selections.invalidateLobby();
		}
	}

	private void queueCapture()
	{
		if (!enabled || captureQueued) return;
		captureQueued = true;
		int queuedGeneration = generation.get();
		// The invocation interface can still be rebuilding inside its script callback.
		// Re-read the complete selection after the client finishes this update, including presets.
		clientThread.invokeLater(() ->
		{
			if (!enabled || generation.get() != queuedGeneration) return;
			captureQueued = false;
			captureInvocations();
		});
	}

	private void refresh()
	{
		if (!enabled || client.getGameState() != GameState.LOGGED_IN) return;
		int region = region();
		// Raid points validate an observed selection; they cannot identify invocations alone.
		selections.update(region, client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL));
		if (region == 13454) queueCapture();
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

	@Provides
	ToaPathwatchConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(ToaPathwatchConfig.class);
	}
}
