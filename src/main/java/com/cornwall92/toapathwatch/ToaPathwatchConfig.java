package com.cornwall92.toapathwatch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(ToaPathwatchConfig.GROUP)
public interface ToaPathwatchConfig extends Config
{
	String GROUP = "toa-path-info";

	@ConfigItem(keyName = "showGlobals", name = "Other raid invocations", description = "Include active general invocations as well as path and boss selections", position = 0)
	default boolean showGlobals() { return false; }

	@ConfigItem(keyName = "showPuzzles", name = "Show in puzzle rooms", description = "Show the path reference in its puzzle room too", position = 1)
	default boolean showPuzzles() { return false; }

	@ConfigItem(keyName = "showScaling", name = "Show path stat increase", description = "Show additional HP/damage scaling from path level only", position = 2)
	default boolean showScaling() { return true; }

	@ConfigItem(keyName = "showPoints", name = "Invocation raid points", description = "Display each invocation's contribution to the raid level", position = 3)
	default boolean showPoints() { return false; }

	@Range(min = 180, max = 450)
	@ConfigItem(keyName = "width", name = "Panel width", description = "Width of the movable reference panel", position = 4)
	default int width() { return 260; }
}
