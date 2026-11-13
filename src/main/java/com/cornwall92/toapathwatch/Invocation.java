package com.cornwall92.toapathwatch;

import java.util.Locale;

/** Explicit UI positions: changing display order must not change these mappings. */
public enum Invocation
{
	TRY_AGAIN(0, "Try Again", 5, null,
		"Allows 10 attempts for the raid. A full party wipe uses one attempt."),
	PERSISTENCE(1, "Persistence", 10, null,
		"Allows 5 attempts for the raid. A full party wipe uses one attempt."),
	SOFTCORE_RUN(2, "Softcore Run", 15, null,
		"Allows 3 attempts for the raid. A full party wipe uses one attempt."),
	HARDCORE_RUN(3, "Hardcore Run", 25, null,
		"Allows only 1 attempt. A full party wipe ends the raid."),
	WALK_FOR_IT(4, "Walk for It", 10, null,
		"Sets a 40-minute target. Missing it lowers the raid level used for rewards."),
	JOG_FOR_IT(5, "Jog for It", 15, null,
		"Sets a 35-minute target. Missing it lowers the raid level used for rewards."),
	RUN_FOR_IT(6, "Run for It", 20, null,
		"Sets a 30-minute target. Missing it lowers the raid level used for rewards."),
	SPRINT_FOR_IT(7, "Sprint for It", 25, null,
		"Sets a 25-minute target. Missing it lowers the raid level used for rewards."),
	NEED_SOME_HELP(8, "Need Some Help?", 15, null,
		"The Helpful Spirit provides 66% of the usual supply quantity."),
	NEED_LESS_HELP(9, "Need Less Help?", 25, null,
		"The Helpful Spirit provides 33% of the usual supply quantity."),
	NO_HELP_NEEDED(10, "No Help Needed", 40, null,
		"The Helpful Spirit provides 10% of the usual supply quantity."),
	WALK_THE_PATH(11, "Walk the Path", 50, null,
		"Completing a path raises levels of remaining paths, increasing enemy health and damage. Boss mechanics strengthen at path levels 2 and 4."),
	PATHSEEKER(12, "Pathseeker", 15, null,
		"All paths start at level 1, increasing enemy health and damage."),
	PATHFINDER(13, "Pathfinder", 40, null,
		"All paths start at level 2, increasing enemy health and damage and strengthening boss mechanics."),
	PATHMASTER(14, "Pathmaster", 50, null,
		"All paths start at level 3, increasing enemy health and damage further, with stronger boss mechanics."),
	QUIET_PRAYERS(15, "Quiet Prayers", 20, null,
		"Protection prayers block 10% less damage inside the raid."),
	DEADLY_PRAYERS(16, "Deadly Prayers", 20, null,
		"Taking damage also drains prayer points equal to 20% of that damage."),
	ON_A_DIET(17, "On a Diet", 15, null,
		"Food and other non-potion healing items cannot restore health inside the raid."),
	DEHYDRATION(18, "Dehydration", 30, null,
		"Potions cannot restore health inside the raid."),
	OVERLY_DRAINING(19, "Overly Draining", 15, null,
		"Every special attack consumes the full special attack energy bar."),
	LIVELY_LARVAE(20, "Lively Larvae", 5, Boss.KEPHRI,
		"More of Kephri's eggs contain Agile Scarabs during her egg attack."),
	MORE_OVERLORDS(21, "More Overlords", 15, Boss.KEPHRI,
		"Kephri summons an extra large scarab during each healing phase."),
	BLOWING_MUD(22, "Blowing Mud", 10, Boss.KEPHRI,
		"Kephri's dung attack targets two players instead of one. It has no extra effect in solo raids."),
	MEDIC(23, "Medic!", 15, Boss.KEPHRI,
		"Shield-restoring scarab swarms keep spawning outside Kephri's healing phases."),
	AERIAL_ASSAULT(24, "Aerial Assault", 10, Boss.KEPHRI,
		"Kephri's regular aerial attack hits harder and covers a larger area."),
	NOT_JUST_A_HEAD(25, "Not Just a Head", 15, Boss.ZEBAK,
		"Zebak gains Blood Barrage and blood clouds. Their damage to players heals him."),
	ARTERIAL_SPRAY(26, "Arterial Spray", 10, Boss.ZEBAK,
		"Zebak's blood magic covers a larger area and restores more of his health."),
	BLOOD_THINNERS(27, "Blood Thinners", 5, Boss.ZEBAK,
		"Zebak summons three blood clouds instead of one, with increased health."),
	UPSET_STOMACH(28, "Upset Stomach", 15, Boss.ZEBAK,
		"Zebak's acid spreads farther, while water jugs clear a smaller area."),
	DOUBLE_TROUBLE(29, "Double Trouble", 20, Boss.AKKHA,
		"Akkha performs two special attacks together."),
	KEEP_BACK(30, "Keep Back", 10, Boss.AKKHA,
		"Akkha's ranged and magic attacks also inflict melee damage on adjacent players."),
	STAY_VIGILANT(31, "Stay Vigilant", 15, Boss.AKKHA,
		"Akkha changes attack styles and protection prayers at random intervals."),
	FEELING_SPECIAL(32, "Feeling Special?", 20, Boss.AKKHA,
		"Akkha's memory sequence is faster, detonations spread in more directions, and trails create extra orbs."),
	MIND_THE_GAP(33, "Mind the Gap!", 10, Boss.BABA,
		"Falling into the pit at Ba-Ba's entrance causes instant death."),
	GOTTA_HAVE_FAITH(34, "Gotta Have Faith", 10, Boss.BABA,
		"Energy from Ba-Ba's sarcophagi deals more damage the more prayer points you are missing."),
	JUNGLE_JAPES(35, "Jungle Japes", 5, Boss.BABA,
		"Baboons in Ba-Ba's room leave banana peels behind when killed."),
	SHAKING_THINGS_UP(36, "Shaking Things Up", 10, Boss.BABA,
		"Ba-Ba's slam shockwave covers a wider area."),
	BOULDERDASH(37, "Boulderdash", 10, Boss.BABA,
		"Ba-Ba releases rolling boulders more frequently."),
	ANCIENT_HASTE(38, "Ancient Haste", 10, Boss.WARDENS,
		"The Wardens charge more quickly during phase 1."),
	ACCELERATION(39, "Acceleration", 10, Boss.WARDENS,
		"During phase 2, the Wardens attack faster and the obelisk charges faster."),
	PENETRATION(40, "Penetration", 10, Boss.WARDENS,
		"The obelisk's phase 2 attacks become more powerful."),
	OVERCLOCKED(41, "Overclocked", 10, Boss.WARDENS,
		"Speeds up Warden attacks in the final phase. Stacks with Overclocked 2."),
	OVERCLOCKED_2(42, "Overclocked 2", 10, Boss.WARDENS,
		"Further speeds up Warden attacks in the final phase. Stacks with Overclocked."),
	INSANITY(43, "Insanity", 50, Boss.WARDENS,
		"In the final phase, floor slams and phantom attacks are faster, and floor tiles disappear faster.");

	public final int uiIndex;
	public final String displayName;
	public final int raidPoints;
	public final Boss boss;
	public final String description;
	private final String matchName;

	Invocation(int uiIndex, String displayName, int raidPoints, Boss boss, String description)
	{
		this.uiIndex = uiIndex;
		this.displayName = displayName;
		this.raidPoints = raidPoints;
		this.boss = boss;
		this.description = description;
		this.matchName = normalize(displayName);
	}

	public boolean isPathInvocation()
	{
		return uiIndex >= 11 && uiIndex <= 14;
	}

	public static Invocation fromName(String name)
	{
		String normalized = normalize(name);
		for (Invocation invocation : values())
		{
			if (invocation.matchName.equals(normalized))
			{
				return invocation;
			}
		}
		return null;
	}

	private static String normalize(String text)
	{
		return text == null ? "" : text.replaceAll("<[^>]*>", "")
			.replace('\u00a0', ' ').trim().toLowerCase(Locale.ROOT);
	}
}
