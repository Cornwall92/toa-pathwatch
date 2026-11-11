package com.cornwall92.toapathwatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Static reference text, not an attack tracker. Sources and limits: RESEARCH.md. */
public final class PathMechanics
{
	private PathMechanics() {}

	public static int tier(int level)
	{
		checkLevel(level);
		return Math.min(level / 2, 2);
	}

	public static int statBonusPercent(int level)
	{
		checkLevel(level);
		return level == 0 ? 0 : 3 + 5 * level;
	}

	private static void checkLevel(int level)
	{
		if (level < 0 || level > 6)
		{
			throw new IllegalArgumentException("Path level must be 0-6");
		}
	}

	public static List<String> describe(Boss boss, int level)
	{
		if (boss == Boss.WARDENS)
		{
			return Collections.singletonList("No separate Warden path level.");
		}
		int tier = tier(level);
		List<String> lines = new ArrayList<>();
		if (tier == 0)
		{
			lines.add("No level-2/4 mechanic upgrades.");
			return Collections.unmodifiableList(lines);
		}
		switch (boss)
		{
			case AKKHA:
				lines.add("Memory Blast: " + (4 + tier) + " steps (base 4).");
				lines.add("Final-phase orbs hit harder; cap 25.");
				break;
			case BABA:
				lines.add(tier == 1 ? "Rockfall drops faster." : "Rockfall drops even faster.");
				lines.add(tier == 1 ? "Sarcophagus blasts cover more tiles." : "Sarcophagus blast area grows again.");
				lines.add("Rolling boulder base HP: " + (tier == 1 ? "27" : "31") + " (before raid scaling).");
				break;
			case KEPHRI:
				lines.add(tier == 1 ? "Faster fireball attacks." : "Fireball attacks speed up again.");
				lines.add(tier == 1 ? "Healing swarms can spawn in threes." : "Triple healing swarms occur more often.");
				break;
			case ZEBAK:
				lines.add(tier == 1 ? "Faster attacks, including enrage." : "Attacks: 5 ticks; enrage: 2 ticks.");
				lines.add("Wave gap: " + (3 - tier) + " tile" + (tier == 1 ? "s." : "."));
				break;
			default:
				throw new IllegalArgumentException("No boss supplied");
		}
		return Collections.unmodifiableList(lines);
	}
}
