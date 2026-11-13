package com.cornwall92.toapathwatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(ToaPathwatchConfig.GROUP)
public interface ToaPathwatchConfig extends Config
{
	String GROUP = "toa-path-info";

	@ConfigItem(keyName = "showInvocations", name = "Show invocations", description = "Show the active invocation list above the path effects", position = 0)
	default boolean showInvocations() { return true; }

	@ConfigItem(keyName = "invocationTooltips", name = "Invocation hover descriptions", description = "Show an invocation's effects when hovering over its name in the panel", position = 1)
	default boolean invocationTooltips() { return true; }

	@ConfigItem(keyName = "showGlobals", name = "Other raid invocations", description = "Include active general invocations as well as path and boss selections", position = 2)
	default boolean showGlobals() { return false; }

	@ConfigItem(keyName = "showPuzzles", name = "Show in puzzle rooms", description = "Show the path reference in its puzzle room too", position = 3)
	default boolean showPuzzles() { return false; }

	@ConfigItem(keyName = "showScaling", name = "Show path stat increase", description = "Show additional HP/damage scaling from path level only", position = 4)
	default boolean showScaling() { return true; }

	@ConfigItem(keyName = "showPoints", name = "Invocation raid points", description = "Display each invocation's contribution to the raid level", position = 5)
	default boolean showPoints() { return false; }

	@Range(min = 180, max = 450)
	@ConfigItem(keyName = "width", name = "Panel width", description = "Width of the movable reference panel", position = 6)
	default int width() { return 260; }

	@Range(min = -1, max = 100)
	@ConfigItem(keyName = "mainOpacity", name = "Main panel opacity (%)", description = "Background opacity: -1 keeps your existing RuneLite setting, 0 is transparent, 100 is solid", position = 7)
	default int mainOpacity() { return -1; }

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "popupOpacity", name = "Hover popup opacity (%)", description = "Background opacity for invocation descriptions: 0 is transparent, 100 is solid", position = 8)
	default int popupOpacity() { return 100; }
}
