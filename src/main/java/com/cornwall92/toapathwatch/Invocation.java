package com.cornwall92.toapathwatch;

import java.util.Locale;

/** Explicit UI positions: changing display order must not change these mappings. */
public enum Invocation
{
	TRY_AGAIN(0, "Try Again", 5, null),
	PERSISTENCE(1, "Persistence", 10, null),
	SOFTCORE_RUN(2, "Softcore Run", 15, null),
	HARDCORE_RUN(3, "Hardcore Run", 25, null),
	WALK_FOR_IT(4, "Walk for It", 10, null),
	JOG_FOR_IT(5, "Jog for It", 15, null),
	RUN_FOR_IT(6, "Run for It", 20, null),
	SPRINT_FOR_IT(7, "Sprint for It", 25, null),
	NEED_SOME_HELP(8, "Need Some Help?", 15, null),
	NEED_LESS_HELP(9, "Need Less Help?", 25, null),
	NO_HELP_NEEDED(10, "No Help Needed", 40, null),
	WALK_THE_PATH(11, "Walk the Path", 50, null),
	PATHSEEKER(12, "Pathseeker", 15, null),
	PATHFINDER(13, "Pathfinder", 40, null),
	PATHMASTER(14, "Pathmaster", 50, null),
	QUIET_PRAYERS(15, "Quiet Prayers", 20, null),
	DEADLY_PRAYERS(16, "Deadly Prayers", 20, null),
	ON_A_DIET(17, "On a Diet", 15, null),
	DEHYDRATION(18, "Dehydration", 30, null),
	OVERLY_DRAINING(19, "Overly Draining", 15, null),
	LIVELY_LARVAE(20, "Lively Larvae", 5, Boss.KEPHRI),
	MORE_OVERLORDS(21, "More Overlords", 15, Boss.KEPHRI),
	BLOWING_MUD(22, "Blowing Mud", 10, Boss.KEPHRI),
	MEDIC(23, "Medic!", 15, Boss.KEPHRI),
	AERIAL_ASSAULT(24, "Aerial Assault", 10, Boss.KEPHRI),
	NOT_JUST_A_HEAD(25, "Not Just a Head", 15, Boss.ZEBAK),
	ARTERIAL_SPRAY(26, "Arterial Spray", 10, Boss.ZEBAK),
	BLOOD_THINNERS(27, "Blood Thinners", 5, Boss.ZEBAK),
	UPSET_STOMACH(28, "Upset Stomach", 15, Boss.ZEBAK),
	DOUBLE_TROUBLE(29, "Double Trouble", 20, Boss.AKKHA),
	KEEP_BACK(30, "Keep Back", 10, Boss.AKKHA),
	STAY_VIGILANT(31, "Stay Vigilant", 15, Boss.AKKHA),
	FEELING_SPECIAL(32, "Feeling Special?", 20, Boss.AKKHA),
	MIND_THE_GAP(33, "Mind the Gap!", 10, Boss.BABA),
	GOTTA_HAVE_FAITH(34, "Gotta Have Faith", 10, Boss.BABA),
	JUNGLE_JAPES(35, "Jungle Japes", 5, Boss.BABA),
	SHAKING_THINGS_UP(36, "Shaking Things Up", 10, Boss.BABA),
	BOULDERDASH(37, "Boulderdash", 10, Boss.BABA),
	ANCIENT_HASTE(38, "Ancient Haste", 10, Boss.WARDENS),
	ACCELERATION(39, "Acceleration", 10, Boss.WARDENS),
	PENETRATION(40, "Penetration", 10, Boss.WARDENS),
	OVERCLOCKED(41, "Overclocked", 10, Boss.WARDENS),
	OVERCLOCKED_2(42, "Overclocked 2", 10, Boss.WARDENS),
	INSANITY(43, "Insanity", 50, Boss.WARDENS);

	public final int uiIndex;
	public final String displayName;
	public final int raidPoints;
	public final Boss boss;
	private final String matchName;

	Invocation(int uiIndex, String displayName, int raidPoints, Boss boss)
	{
		this.uiIndex = uiIndex;
		this.displayName = displayName;
		this.raidPoints = raidPoints;
		this.boss = boss;
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
