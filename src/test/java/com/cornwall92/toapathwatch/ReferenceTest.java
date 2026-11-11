package com.cornwall92.toapathwatch;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReferenceTest
{
	private Map<Invocation, Boolean> blank()
	{
		Map<Invocation, Boolean> states = new EnumMap<>(Invocation.class);
		for (Invocation inv : Invocation.values()) states.put(inv, false);
		return states;
	}

	@Test public void completeZeroRaidIsKnownEmpty()
	{
		InvocationSelection selection = InvocationSelection.validate(blank(), 0);
		assertNotNull(selection);
		assertTrue(selection.active.isEmpty());
	}

	@Test public void incompleteReadIsUnknown()
	{
		Map<Invocation, Boolean> states = blank();
		states.remove(Invocation.INSANITY);
		assertNull(InvocationSelection.validate(states, 0));
	}

	@Test public void greyLoadingFrameCannotOverwriteNonzeroRaid()
	{
		assertNull(InvocationSelection.validate(blank(), 300));
	}

	@Test public void allInvocationsContributeToScoreIncludingHiddenGlobals()
	{
		Map<Invocation, Boolean> states = blank();
		states.put(Invocation.PATHFINDER, true);
		states.put(Invocation.WALK_THE_PATH, true);
		states.put(Invocation.NEED_LESS_HELP, true);
		InvocationSelection selection = InvocationSelection.validate(states, 115);
		assertNotNull(selection);
		assertEquals(3, selection.active.size());
		assertNull(InvocationSelection.validate(states, 110));
	}

	@Test public void unknownStateIsNotInactive()
	{
		Map<Invocation, Boolean> states = blank();
		states.put(Invocation.KEEP_BACK, null);
		assertNull(InvocationSelection.validate(states, 0));
	}

	@Test public void namesKeepPunctuationAndIgnoreGameColorTags()
	{
		assertSame(Invocation.FEELING_SPECIAL, Invocation.fromName("<col=d39034>Feeling Special?</col>"));
		assertSame(Invocation.MEDIC, Invocation.fromName("Medic!"));
		assertSame(Invocation.MIND_THE_GAP, Invocation.fromName("Mind the Gap!"));
		assertNull(Invocation.fromName("New unrecognised invocation"));
	}

	@Test public void catalogHasAll44UniquePositions()
	{
		assertEquals(44, Invocation.values().length);
		Set<Integer> positions = new HashSet<>();
		for (Invocation inv : Invocation.values())
		{
			assertTrue(positions.add(inv.uiIndex));
			assertSame(inv, Invocation.fromName(inv.displayName));
		}
		for (int i = 0; i < 44; i++) assertTrue(positions.contains(i));
	}

	@Test public void mechanicsChangeOnlyAtTwoAndFour()
	{
		assertEquals(0, PathMechanics.tier(1));
		assertEquals(1, PathMechanics.tier(2));
		assertEquals(1, PathMechanics.tier(3));
		assertEquals(2, PathMechanics.tier(4));
		for (Boss boss : Boss.values())
		{
			assertEquals(PathMechanics.describe(boss, 4), PathMechanics.describe(boss, 6));
		}
	}

	@Test public void scalingContinuesBeyondLastMechanicTier()
	{
		int[] expected = {0, 8, 13, 18, 23, 28, 33};
		for (int i = 0; i < expected.length; i++) assertEquals(expected[i], PathMechanics.statBonusPercent(i));
	}

	@Test(expected = IllegalArgumentException.class) public void missingLevelIsNotSilentlyTreatedAsZero()
	{
		PathMechanics.describe(Boss.AKKHA, -1);
	}

	@Test public void wardensHaveNoInventedPathTier()
	{
		assertEquals(-1, Boss.WARDENS.levelVarbit);
		assertEquals("No separate Warden path level.", PathMechanics.describe(Boss.WARDENS, -1).get(0));
		assertSame(Boss.WARDENS, Boss.forRegion(15184, false));
		assertSame(Boss.WARDENS, Boss.forRegion(15696, false));
	}

	@Test public void puzzleVisibilityIsOptional()
	{
		assertNull(Boss.forRegion(14674, false));
		assertSame(Boss.AKKHA, Boss.forRegion(14674, true));
		assertSame(Boss.AKKHA, Boss.forRegion(14676, false));
		assertTrue(Boss.isRaidRegion(14674));
		assertFalse(Boss.isRaidRegion(13454));
	}
}
