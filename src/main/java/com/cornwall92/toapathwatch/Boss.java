package com.cornwall92.toapathwatch;

import net.runelite.api.gameval.VarbitID;

public enum Boss
{
	KEPHRI("Kephri", "Scabaras", 14164, 14162, VarbitID.TOA_CLIENT_SCABARAS_LEVEL, 1),
	ZEBAK("Zebak", "Crondis", 15700, 15698, VarbitID.TOA_CLIENT_CRONDIS_LEVEL, 4),
	AKKHA("Akkha", "Het", 14676, 14674, VarbitID.TOA_CLIENT_HET_LEVEL, 2),
	BABA("Ba-Ba", "Apmeken", 15188, 15186, VarbitID.TOA_CLIENT_APMEKEN_LEVEL, 3),
	WARDENS("Wardens", "", 15184, 15696, -1, -1);

	public final String displayName;
	public final String pathName;
	public final int bossRegion;
	public final int otherRegion;
	public final int levelVarbit;
	/** TOA_CLIENT_CURRENT_PATH encoding used by the game's toa_hud_draw script. */
	public final int pathId;

	Boss(String displayName, String pathName, int bossRegion, int otherRegion, int levelVarbit, int pathId)
	{
		this.displayName = displayName;
		this.pathName = pathName;
		this.bossRegion = bossRegion;
		this.otherRegion = otherRegion;
		this.levelVarbit = levelVarbit;
		this.pathId = pathId;
	}

	public static Boss forRegion(int region, boolean includePuzzles)
	{
		for (Boss boss : values())
		{
			if (region == boss.bossRegion || (region == boss.otherRegion && (includePuzzles || boss == WARDENS)))
			{
				return boss;
			}
		}
		return null;
	}

	public static boolean isRaidRegion(int region)
	{
		return region == 14160 || region == 14672 || forRegion(region, true) != null;
	}
}
