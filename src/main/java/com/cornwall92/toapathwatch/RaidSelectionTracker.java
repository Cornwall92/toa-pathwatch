package com.cornwall92.toapathwatch;

/**
 * Client-thread state for a lobby capture and its frozen raid selection.
 * Unknown geography is a temporary absence of evidence, not a confirmed exit.
 */
public final class RaidSelectionTracker
{
	private static final int LOBBY_REGION = 13454;
	private static final int NEXUS_REGION = 14160;
	private static final int UNKNOWN_GRACE_TICKS = 3;
	private static final int ENTRY_SCORE_ATTEMPTS = 5;

	private boolean inRaid;
	private InvocationSelection lobbyCandidate;
	private InvocationSelection entryCandidate;
	private InvocationSelection selection;
	private int unknownTicks;
	private int scoreAttemptsRemaining;

	/** Call only with a selection read from the current party's lobby interface. */
	public void capture(InvocationSelection captured)
	{
		if (!inRaid)
		{
			lobbyCandidate = captured;
		}
	}

	public void invalidateLobby()
	{
		lobbyCandidate = null;
	}

	/**
	 * Called once per game tick. A region of -1 means the local position is unknown.
	 * There are five score observations, including the first entry tick. Subsequent
	 * observations are permitted only in the nexus; entering a path closes the window.
	 */
	public void update(int region, int raidPoints)
	{
		if (region == -1)
		{
			if (++unknownTicks > UNKNOWN_GRACE_TICKS)
			{
				reset();
			}
			return;
		}
		unknownTicks = 0;

		if (!Boss.isRaidRegion(region))
		{
			if (inRaid || region != LOBBY_REGION)
			{
				reset();
			}
			return;
		}

		boolean entering = !inRaid;
		if (entering)
		{
			inRaid = true;
			entryCandidate = lobbyCandidate;
			lobbyCandidate = null;
			selection = null;
			scoreAttemptsRemaining = ENTRY_SCORE_ATTEMPTS;
		}

		if (entryCandidate == null)
		{
			return;
		}
		if (!entering && region != NEXUS_REGION)
		{
			clearEntryCandidate();
			return;
		}

		if (entryCandidate.raidPoints == raidPoints)
		{
			selection = entryCandidate;
			clearEntryCandidate();
		}
		else if (--scoreAttemptsRemaining == 0)
		{
			clearEntryCandidate();
		}
	}

	private void clearEntryCandidate()
	{
		entryCandidate = null;
		scoreAttemptsRemaining = 0;
	}

	public InvocationSelection getSelection()
	{
		return selection;
	}

	public boolean isInRaid()
	{
		return inRaid;
	}

	public void reset()
	{
		inRaid = false;
		lobbyCandidate = null;
		entryCandidate = null;
		selection = null;
		unknownTicks = 0;
		scoreAttemptsRemaining = 0;
	}
}
