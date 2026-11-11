package com.cornwall92.toapathwatch;

import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class RaidSelectionTrackerTest
{
	private static final int LOBBY = 13454;
	private static final int NEXUS = 14160;
	private static final int AKKHA = 14676;
	private static final int OUTSIDE = 12850;

	private InvocationSelection selection(Invocation... active)
	{
		Map<Invocation, Boolean> states = new EnumMap<>(Invocation.class);
		for (Invocation invocation : Invocation.values()) states.put(invocation, false);
		for (Invocation invocation : active) states.put(invocation, true);
		return InvocationSelection.validate(states, null);
	}

	@Test public void repeatedLobbyTicksPreserveFreshCapture()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		InvocationSelection captured = selection(Invocation.WALK_THE_PATH);
		tracker.capture(captured);
		tracker.update(LOBBY, 0);
		tracker.update(LOBBY, 0);
		tracker.update(NEXUS, 50);
		assertSame(captured, tracker.getSelection());
	}

	@Test public void delayedRaidLevelCanValidateInNexus()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		InvocationSelection captured = selection(Invocation.PATHFINDER);
		tracker.capture(captured);
		tracker.update(NEXUS, 0);
		assertTrue(tracker.isInRaid());
		assertNull(tracker.getSelection());
		tracker.update(NEXUS, 40);
		assertSame(captured, tracker.getSelection());
	}

	@Test public void entryValidationExpiresAfterFiveObservations()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		tracker.capture(selection(Invocation.WALK_THE_PATH));
		for (int i = 0; i < 5; i++) tracker.update(NEXUS, 0);
		tracker.update(NEXUS, 50);
		assertNull(tracker.getSelection());
	}

	@Test public void entryValidationStillWorksOnFifthObservation()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		InvocationSelection captured = selection(Invocation.WALK_THE_PATH);
		tracker.capture(captured);
		for (int i = 0; i < 4; i++) tracker.update(NEXUS, 0);
		tracker.update(NEXUS, 50);
		assertSame(captured, tracker.getSelection());
	}

	@Test public void leavingNexusClosesPendingValidation()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		tracker.capture(selection(Invocation.WALK_THE_PATH));
		tracker.update(NEXUS, 0);
		tracker.update(AKKHA, 50);
		tracker.update(NEXUS, 50);
		assertNull(tracker.getSelection());
	}

	@Test public void temporaryUnknownRegionDoesNotEraseRaidCapture()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		InvocationSelection captured = selection(Invocation.WALK_THE_PATH);
		tracker.capture(captured);
		tracker.update(NEXUS, 50);
		for (int i = 0; i < 3; i++) tracker.update(-1, 0);
		assertTrue(tracker.isInRaid());
		tracker.update(AKKHA, 50);
		assertSame(captured, tracker.getSelection());
	}

	@Test public void persistentUnknownRegionEventuallyClearsRaid()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		tracker.capture(selection(Invocation.WALK_THE_PATH));
		tracker.update(NEXUS, 50);
		for (int i = 0; i < 4; i++) tracker.update(-1, 0);
		assertFalse(tracker.isInRaid());
		assertNull(tracker.getSelection());
		tracker.update(AKKHA, 50);
		assertNull(tracker.getSelection());
	}

	@Test public void confirmedExitClearsImmediatelyAndCannotReusePriorCapture()
	{
		for (int destination : new int[]{LOBBY, OUTSIDE})
		{
			RaidSelectionTracker tracker = new RaidSelectionTracker();
			tracker.capture(selection(Invocation.WALK_THE_PATH));
			tracker.update(NEXUS, 50);
			tracker.update(destination, 50);
			assertFalse(tracker.isInRaid());
			assertNull(tracker.getSelection());
			tracker.update(NEXUS, 50);
			assertNull(tracker.getSelection());
		}
	}

	@Test public void frozenCaptureSurvivesLaterScoreChangesAndCaptureAttempts()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		InvocationSelection captured = selection(Invocation.RUN_FOR_IT);
		tracker.capture(captured);
		tracker.update(NEXUS, 20);
		tracker.capture(selection(Invocation.WALK_THE_PATH));
		tracker.invalidateLobby();
		tracker.update(AKKHA, 0);
		tracker.update(NEXUS, 50);
		assertSame(captured, tracker.getSelection());
	}

	@Test public void newPresetReplacesPreviousLobbySelection()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		tracker.capture(selection(Invocation.RUN_FOR_IT));
		InvocationSelection replacement = selection(Invocation.WALK_THE_PATH);
		tracker.capture(replacement);
		tracker.update(NEXUS, 50);
		assertSame(replacement, tracker.getSelection());
	}

	@Test public void invalidatedGridDoesNotReuseEarlierPreset()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		tracker.capture(selection(Invocation.RUN_FOR_IT));
		tracker.capture(null);
		tracker.update(NEXUS, 20);
		assertNull(tracker.getSelection());
	}

	@Test public void midRaidEnablingDoesNotInventSelection()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		tracker.update(AKKHA, 300);
		assertTrue(tracker.isInRaid());
		assertNull(tracker.getSelection());
	}

	@Test public void zeroInvocationRaidIsKnownRatherThanMissing()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		InvocationSelection empty = selection();
		tracker.capture(empty);
		tracker.update(NEXUS, 0);
		assertSame(empty, tracker.getSelection());
		assertTrue(tracker.getSelection().active.isEmpty());
	}

	@Test public void resetClearsFrozenAndPendingData()
	{
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		tracker.capture(selection(Invocation.WALK_THE_PATH));
		tracker.update(NEXUS, 0);
		tracker.reset();
		tracker.update(NEXUS, 50);
		assertNull(tracker.getSelection());
		tracker.reset();
		assertFalse(tracker.isInRaid());
	}
}
