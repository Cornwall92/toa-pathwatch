package com.cornwall92.toapathwatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class ToaPathwatchOverlay extends OverlayPanel
{
	private static final Color GOLD = new Color(255, 195, 85);
	private static final Color GREEN = new Color(130, 220, 155);
	private final ToaPathwatchPlugin plugin;
	private final ToaPathwatchConfig config;

	@Inject
	ToaPathwatchOverlay(ToaPathwatchPlugin plugin, ToaPathwatchConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setResizable(false);
	}

	private void line(String text, Color color)
	{
		panelComponent.getChildren().add(LineComponent.builder().left(text).leftColor(color).build());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Boss boss = plugin.getBoss();
		if (boss == null)
		{
			String status = plugin.getLobbyStatus();
			if (status == null) return null;
			panelComponent.setPreferredSize(new Dimension(config.width(), 0));
			panelComponent.getChildren().add(TitleComponent.builder().text("ToA Pathwatch 0.1.3").color(GOLD).build());
			line(status, Color.LIGHT_GRAY);
			return super.render(graphics);
		}
		int level = plugin.getPathLevel();
		panelComponent.setPreferredSize(new Dimension(config.width(), 0));
		String title = boss.displayName + (boss == Boss.WARDENS ? "" : " - Path " + (level < 0 ? "?" : level));
		panelComponent.getChildren().add(TitleComponent.builder().text(title).color(GOLD).build());
		InvocationSelection selection = plugin.getSelection();
		line("Active invocations", GOLD);
		if (selection == null)
		{
			line("Not captured. Open your party's invocation tab before entering the raid.", Color.LIGHT_GRAY);
		}
		else
		{
			boolean any = false;
			for (Invocation invocation : Invocation.values())
			{
				if (selection.active.contains(invocation) && (invocation.boss == boss || invocation.isPathInvocation()
					|| (config.showGlobals() && invocation.boss == null)))
				{
					line(invocation.displayName + (config.showPoints() ? " (+" + invocation.raidPoints + ")" : ""), GREEN);
					any = true;
				}
			}
			if (!any) line("None relevant enabled.", Color.LIGHT_GRAY);
		}
		line("Path effects", GOLD);
		if (boss == Boss.WARDENS)
		{
			line("No separate Warden path level.", Color.LIGHT_GRAY);
		}
		else if (level < 0)
		{
			line("Waiting for path level.", Color.LIGHT_GRAY);
		}
		else
		{
			if (config.showScaling()) line("Path HP / damage: +" + PathMechanics.statBonusPercent(level) + "%", Color.WHITE);
			for (String effect : PathMechanics.describe(boss, level)) line(effect, Color.WHITE);
		}
		return super.render(graphics);
	}
}
