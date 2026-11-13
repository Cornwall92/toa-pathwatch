package com.cornwall92.toapathwatch;

import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.*;

/** Literal script-6729 payloads, independent of the plugin's UI indexes and point totals. */
public class PartyBuildReaderTest
{
	private Object[] partyBuild()
	{
		// CS2 parameters: role, party argument, count, score, tab, three 31-bit words.
		// Cache bits: Hardcore 4, Walk the Path 12, Double Trouble 30,
		// Keep Back 31, Stay Vigilant 32, Feeling Special 33, Insanity 44.
		return new Object[]{6729, 2, 12345, 7, 190, 2, 0x40001010, 0x2007, 0};
	}

	@Test public void leaderPayloadDecodesIndependentFixtureAcrossWordBoundary()
	{
		InvocationSelection result = InvocationReader.readPartyBuild(partyBuild());
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.HARDCORE_RUN, Invocation.WALK_THE_PATH,
			Invocation.DOUBLE_TROUBLE, Invocation.KEEP_BACK, Invocation.STAY_VIGILANT,
			Invocation.FEELING_SPECIAL, Invocation.INSANITY), result.active);
		assertEquals(190, result.raidPoints);
	}

	@Test public void currentPartyMemberUsesTheSameSelectionWords()
	{
		Object[] args = partyBuild();
		args[1] = 1;
		InvocationSelection result = InvocationReader.readPartyBuild(args);
		assertNotNull(result);
		assertEquals(7, result.active.size());
		assertTrue(result.active.contains(Invocation.KEEP_BACK));
		assertEquals(190, result.raidPoints);
	}

	@Test public void firstInvocationUsesBitOneRatherThanBitZero()
	{
		InvocationSelection result = InvocationReader.readPartyBuild(
			new Object[]{6729, 1, 0, 1, 5, 2, 0x2, 0, 0});
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.TRY_AGAIN), result.active);
	}

	@Test public void bitThirtyRemainsInFirstWord()
	{
		InvocationSelection result = InvocationReader.readPartyBuild(
			new Object[]{6729, 2, 0, 1, 20, 2, 0x40000000, 0, 0});
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.DOUBLE_TROUBLE), result.active);
	}

	@Test public void bitThirtyOneStartsSecondWordAtBitZero()
	{
		InvocationSelection result = InvocationReader.readPartyBuild(
			new Object[]{6729, 2, 0, 1, 10, 2, 0, 0x1, 0});
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.KEEP_BACK), result.active);
	}

	@Test public void lastOrdinaryInvocationUsesSecondWordBitThirteen()
	{
		InvocationSelection result = InvocationReader.readPartyBuild(
			new Object[]{6729, 2, 0, 1, 50, 2, 0, 0x2000, 0});
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.INSANITY), result.active);
	}

	@Test public void zeroInvocationsAreKnownForBothCurrentPartyRoles()
	{
		for (int role : new int[]{1, 2})
		{
			InvocationSelection result = InvocationReader.readPartyBuild(
				new Object[]{6729, role, 0, 0, 0, 2, 0, 0, 0});
			assertNotNull(result);
			assertTrue(result.active.isEmpty());
			assertEquals(0, result.raidPoints);
		}
	}

	@Test public void otherPartyAndApplicantRolesAreRejectedEvenWithValidSelection()
	{
		for (int role : new int[]{-1, 0, 3, 4, 5, Integer.MAX_VALUE})
		{
			Object[] args = partyBuild();
			args[1] = role;
			assertNull("role " + role, InvocationReader.readPartyBuild(args));
		}
	}

	@Test public void readerAcceptsAllKnownTabsForCallerToApplyVisibilityPolicy()
	{
		for (int tab = 0; tab <= 3; tab++)
		{
			Object[] args = partyBuild();
			args[5] = tab;
			assertNotNull("tab " + tab, InvocationReader.readPartyBuild(args));
		}
		for (int tab : new int[]{-1, 4, Integer.MAX_VALUE})
		{
			Object[] args = partyBuild();
			args[5] = tab;
			assertNull("tab " + tab, InvocationReader.readPartyBuild(args));
		}
	}

	@Test public void unrelatedIntegerPartyArgumentDoesNotChangeSelection()
	{
		for (int value : new int[]{-1, 0, 0x12345678})
		{
			Object[] args = partyBuild();
			args[2] = value;
			InvocationSelection result = InvocationReader.readPartyBuild(args);
			assertNotNull(result);
			assertEquals(190, result.raidPoints);
		}
	}

	@Test public void mismatchingNegativeAndExcessiveActiveCountsAreRejected()
	{
		for (int count : new int[]{-1, 0, 6, 8, 45, Integer.MAX_VALUE})
		{
			Object[] args = partyBuild();
			args[3] = count;
			assertNull("count " + count, InvocationReader.readPartyBuild(args));
		}
	}

	@Test public void mismatchingNegativeAndExcessiveScoresAreRejected()
	{
		for (int points : new int[]{-1, 0, 189, 191, Integer.MAX_VALUE})
		{
			Object[] args = partyBuild();
			args[4] = points;
			assertNull("points " + points, InvocationReader.readPartyBuild(args));
		}
	}

	@Test public void reservedAndUnsupportedBitsCannotBeSilentlyDiscarded()
	{
		int[][] unsupported = {
			{6, 0x1},          // First word bit zero is reserved.
			{6, 0x80000000},   // The words use 31 bits, not 32.
			{7, 0x4000},       // Blazing Tombs I is outside the supported ordinary set.
			{7, 0x8000},       // Blazing Tombs II is likewise unsupported.
			{7, 0x40000000},
			{7, 0x80000000},
			{8, 0x1},
			{8, 0x80000000}
		};
		for (int[] bit : unsupported)
		{
			Object[] args = partyBuild();
			args[bit[0]] = (Integer) args[bit[0]] | bit[1];
			assertNull("argument " + bit[0] + ", mask " + bit[1], InvocationReader.readPartyBuild(args));
		}
	}

	@Test public void nullShortAndExtraArgumentPayloadsAreRejected()
	{
		assertNull(InvocationReader.readPartyBuild(null));
		for (int length : new int[]{0, 1, 5, 8, 10})
		{
			Object[] args = Arrays.copyOf(partyBuild(), length);
			if (length == 10) args[9] = 0;
			assertNull("length " + length, InvocationReader.readPartyBuild(args));
		}
	}

	@Test public void everyArgumentRequiresAnIntegerWithoutCoercion()
	{
		for (int index = 0; index < 9; index++)
		{
			for (Object invalid : new Object[]{null, "0", 0L, Boolean.FALSE})
			{
				Object[] args = partyBuild();
				args[index] = invalid;
				assertNull("argument " + index, InvocationReader.readPartyBuild(args));
			}
		}
	}

	@Test public void differentScriptAndReorderedParametersAreRejected()
	{
		Object[] wrongScript = partyBuild();
		wrongScript[0] = 6754;
		assertNull(InvocationReader.readPartyBuild(wrongScript));
		Object[] swappedCountAndScore = partyBuild();
		swappedCountAndScore[3] = 190;
		swappedCountAndScore[4] = 7;
		assertNull(InvocationReader.readPartyBuild(swappedCountAndScore));
		Object[] swappedWords = partyBuild();
		swappedWords[6] = 0x2007;
		swappedWords[7] = 0x40001010;
		assertNull(InvocationReader.readPartyBuild(swappedWords));
	}

	@Test public void laterEmptyPresetDoesNotMutateEarlierSnapshot()
	{
		InvocationSelection first = InvocationReader.readPartyBuild(partyBuild());
		InvocationSelection cleared = InvocationReader.readPartyBuild(
			new Object[]{6729, 2, 12345, 0, 0, 2, 0, 0, 0});
		assertNotNull(first);
		assertNotNull(cleared);
		assertEquals(7, first.active.size());
		assertEquals(190, first.raidPoints);
		assertTrue(cleared.active.isEmpty());
	}
}
