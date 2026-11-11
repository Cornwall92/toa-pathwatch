package com.cornwall92.toapathwatch;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Exercises the actual widget reader with synthetic interface contents, not game input. */
public class InvocationReaderTest
{
	private static final int ACTIVE = 0xD39034;
	private static final int INACTIVE = 0x808080;

	private static final class Grid
	{
		private final Client client = mock(Client.class);
		private final Widget parent = mock(Widget.class);
		private final Widget score = mock(Widget.class);
		private final boolean leader;
		private Widget[] children;

		private Grid(boolean leader, Invocation... active)
		{
			this.leader = leader;
			when(client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT)).thenReturn(parent);
			when(client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL)).thenReturn(score);
			populate(active);
		}

		private void populate(Invocation... active)
		{
			Set<Invocation> enabled = EnumSet.noneOf(Invocation.class);
			enabled.addAll(Arrays.asList(active));
			children = new Widget[Invocation.values().length * 3];
			int points = 0;
			for (Invocation invocation : Invocation.values())
			{
				boolean selected = enabled.contains(invocation);
				int index = invocation.uiIndex * 3;
				Widget background = mock(Widget.class);
				Widget label = mock(Widget.class);
				children[index] = background;
				children[index + 1] = mock(Widget.class);
				children[index + 2] = label;
				when(label.getText()).thenReturn(invocation.displayName);
				when(label.getTextColor()).thenReturn(selected ? ACTIVE : INACTIVE);
				if (leader)
				{
					when(background.getOnOpListener()).thenReturn(new Object[]{6729, 0, 0, selected ? 1 : 0});
				}
				if (selected) points += invocation.raidPoints;
			}
			when(parent.getChildren()).thenReturn(children);
			when(score.getText()).thenReturn("Raid Level: " + points);
		}
	}

	@Test public void leaderButtonStateOverridesContradictoryLabelColor()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH, Invocation.FEELING_SPECIAL);
		for (Invocation invocation : Invocation.values())
		{
			boolean selected = invocation == Invocation.WALK_THE_PATH || invocation == Invocation.FEELING_SPECIAL;
			when(grid.children[invocation.uiIndex * 3 + 2].getTextColor()).thenReturn(selected ? INACTIVE : ACTIVE);
		}
		InvocationSelection result = InvocationReader.read(grid.client);
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH, Invocation.FEELING_SPECIAL), result.active);
	}

	@Test public void nonleaderColorReadRequiresAndMatchesFullPartyScore()
	{
		Grid grid = new Grid(false, Invocation.PATHFINDER, Invocation.MEDIC, Invocation.NEED_LESS_HELP);
		InvocationSelection result = InvocationReader.read(grid.client);
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.PATHFINDER, Invocation.MEDIC, Invocation.NEED_LESS_HELP), result.active);
		assertEquals(80, result.raidPoints);
	}

	@Test public void completeZeroSelectionIsKnownForNonleader()
	{
		Grid grid = new Grid(false);
		InvocationSelection result = InvocationReader.read(grid.client);
		assertNotNull(result);
		assertTrue(result.active.isEmpty());
		assertEquals(0, result.raidPoints);
	}

	@Test public void missingLabelRejectsPartialGrid()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH);
		grid.children[Invocation.INSANITY.uiIndex * 3 + 2] = null;
		assertNull(InvocationReader.read(grid.client));
	}

	@Test public void duplicateLabelRejectsAmbiguousGrid()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH);
		Widget[] duplicated = Arrays.copyOf(grid.children, grid.children.length + 3);
		duplicated[duplicated.length - 1] = grid.children[Invocation.WALK_THE_PATH.uiIndex * 3 + 2];
		when(grid.parent.getChildren()).thenReturn(duplicated);
		assertNull(InvocationReader.read(grid.client));
	}

	@Test public void colorCaptureWithoutScoreIsUnknown()
	{
		Grid grid = new Grid(false, Invocation.WALK_THE_PATH);
		when(grid.client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL)).thenReturn(null);
		assertNull(InvocationReader.read(grid.client));
	}

	@Test public void alternateScoreWidgetCanValidateColorCapture()
	{
		Grid grid = new Grid(false, Invocation.WALK_THE_PATH);
		when(grid.client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL)).thenReturn(null);
		when(grid.client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL_INVOCATIONS)).thenReturn(grid.score);
		assertNotNull(InvocationReader.read(grid.client));
	}

	@Test public void mismatchingScoreRejectsBothLeaderAndNonleaderCapture()
	{
		for (boolean leader : new boolean[]{true, false})
		{
			Grid grid = new Grid(leader, Invocation.WALK_THE_PATH);
			when(grid.score.getText()).thenReturn("300");
			assertNull(InvocationReader.read(grid.client));
		}
	}

	@Test public void presetReplacementRemovesPreviouslyEnabledInvocations()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH, Invocation.PATHFINDER);
		RaidSelectionTracker tracker = new RaidSelectionTracker();
		InvocationSelection first = InvocationReader.read(grid.client);
		tracker.capture(first);
		grid.populate(Invocation.PATHMASTER, Invocation.MIND_THE_GAP);
		InvocationSelection second = InvocationReader.read(grid.client);
		assertNotNull(second);
		tracker.capture(second);
		tracker.update(14160, 60);
		assertEquals(EnumSet.of(Invocation.PATHMASTER, Invocation.MIND_THE_GAP), tracker.getSelection().active);
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH, Invocation.PATHFINDER), first.active);
	}

	@Test public void unavailableOrHiddenGridIsUnknown()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH);
		when(grid.parent.isHidden()).thenReturn(true);
		assertNull(InvocationReader.read(grid.client));
		when(grid.parent.isHidden()).thenReturn(false);
		when(grid.parent.getChildren()).thenReturn(null);
		assertNull(InvocationReader.read(grid.client));
		when(grid.client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT)).thenReturn(null);
		assertNull(InvocationReader.read(grid.client));
	}

	@Test public void numericReaderAcceptsHudAndFormattedPartyScore()
	{
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("4");
		assertEquals(Integer.valueOf(4), InvocationReader.number(widget));
		when(widget.getText()).thenReturn(" <col=ff981f>Raid Level: 300</col> ");
		assertEquals(Integer.valueOf(300), InvocationReader.number(widget));
		when(widget.getText()).thenReturn("Raid level: 150");
		assertEquals(Integer.valueOf(150), InvocationReader.number(widget));
	}

	@Test public void numericReaderRejectsHiddenMissingMalformedAndOverflowValues()
	{
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("4");
		when(widget.isHidden()).thenReturn(true);
		assertNull(InvocationReader.number(widget));
		when(widget.isHidden()).thenReturn(false);
		for (String value : new String[]{null, "", "4 / 6", "not a level", "-1", "9999999999999999999"})
		{
			when(widget.getText()).thenReturn(value);
			assertNull(InvocationReader.number(widget));
		}
		assertNull(InvocationReader.number(null));
	}
}
