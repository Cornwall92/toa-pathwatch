package com.cornwall92.toapathwatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.TooltipComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class ToaPathwatchOverlay extends OverlayPanel
{
	private static final Color GOLD = new Color(255, 195, 85);
	private static final Color GREEN = new Color(130, 220, 155);
	private static final int TOOLTIP_WIDTH = 300;
	private final ToaPathwatchPlugin plugin;
	private final ToaPathwatchConfig config;
	private final Client client;
	private final TooltipManager tooltipManager;
	private final RuneLiteConfig runeLiteConfig;
	private final Map<Invocation, LineComponent> invocationRows = new EnumMap<>(Invocation.class);

	@Inject
	ToaPathwatchOverlay(ToaPathwatchPlugin plugin, ToaPathwatchConfig config, Client client,
		TooltipManager tooltipManager, RuneLiteConfig runeLiteConfig)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.runeLiteConfig = runeLiteConfig;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setResizable(false);
	}

	private LineComponent line(String text, Color color)
	{
		LineComponent row = LineComponent.builder().left(text).leftColor(color).build();
		panelComponent.getChildren().add(row);
		return row;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		invocationRows.clear();
		// Resolve each frame so both local settings and RuneLite's background colour update live.
		setPreferredColor(withOpacity(runeLiteConfig.overlayBackgroundColor(), config.mainOpacity()));
		Boss boss = plugin.getBoss();
		if (boss == null)
		{
			String status = plugin.getLobbyStatus();
			if (status == null) return null;
			panelComponent.setPreferredSize(new Dimension(config.width(), 0));
			panelComponent.getChildren().add(TitleComponent.builder().text("ToA Pathwatch 0.1.5").color(GOLD).build());
			line(status, Color.LIGHT_GRAY);
			return super.render(graphics);
		}
		int level = plugin.getPathLevel();
		panelComponent.setPreferredSize(new Dimension(config.width(), 0));
		String title = boss.displayName + (boss == Boss.WARDENS ? "" : " - Path " + (level < 0 ? "?" : level));
		panelComponent.getChildren().add(TitleComponent.builder().text(title).color(GOLD).build());
		if (config.showInvocations()) renderInvocations(boss);
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
		Dimension dimension = super.render(graphics);
		// Components have their final bounds only after layout, including wrapped names.
		if (config.invocationTooltips() && !invocationRows.isEmpty() && !client.isMenuOpen())
		{
			renderTooltip(graphics.getFontMetrics(runeLiteConfig.tooltipFont().getFont()));
		}
		return dimension;
	}

	private void renderInvocations(Boss boss)
	{
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
					invocationRows.put(invocation, line(invocation.displayName
						+ (config.showPoints() ? " (+" + invocation.raidPoints + ")" : ""), GREEN));
					any = true;
				}
			}
			if (!any) line("None relevant enabled.", Color.LIGHT_GRAY);
		}
	}

	private void renderTooltip(FontMetrics metrics)
	{
		Point mouse = client.getMouseCanvasPosition();
		// RuneLite positions the overlay in canvas coordinates before calling render.
		int x = mouse.getX() - getBounds().x;
		int y = mouse.getY() - getBounds().y;
		for (Map.Entry<Invocation, LineComponent> entry : invocationRows.entrySet())
		{
			if (entry.getValue().getBounds().contains(x, y))
			{
				Invocation invocation = entry.getKey();
				StringBuilder text = new StringBuilder("<col=ffc355>").append(invocation.displayName).append("</col><br>");
				int width = 0;
				int space = metrics.stringWidth(" ");
				for (String word : invocation.description.split(" "))
				{
					int wordWidth = metrics.stringWidth(word);
					if (width > 0)
					{
						if (width + space + wordWidth > TOOLTIP_WIDTH)
						{
							text.append("<br>");
							width = 0;
						}
						else
						{
							text.append(' ');
							width += space;
						}
					}
					text.append(word);
					width += wordWidth;
				}
				Tooltip tooltip = new Tooltip(text.toString());
				TooltipComponent component = new TooltipComponent();
				component.setText(tooltip.getText());
				component.setBackgroundColor(withOpacity(runeLiteConfig.overlayBackgroundColor(), config.popupOpacity()));
				// A supplied TooltipComponent retains its colour; string/PanelComponent tooltips
				// are recoloured by RuneLite's TooltipOverlay using the global transparency.
				tooltip.setComponent(component);
				tooltipManager.add(tooltip);
				return;
			}
		}
	}

	private static Color withOpacity(Color color, int opacity)
	{
		if (opacity < 0) return color;
		int alpha = Math.round(Math.min(opacity, 100) * 255 / 100f);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}
