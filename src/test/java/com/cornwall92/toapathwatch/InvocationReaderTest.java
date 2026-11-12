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
	// Script 6750 colours; script 6754 callback arguments; script 6729 header format.
	private static final int ACTIVE = 0xD39F34;
	private static final int INACTIVE = 0x9F9F9F;

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
					when(background.getOnOpListener()).thenReturn(new Object[]{6754, 1, 0, selected ? 1 : 0,
						2, index, index + 1, index + 2, InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT, 0});
				}
				if (selected) points += invocation.raidPoints;
			}
			when(parent.getChildren()).thenReturn(children);
			when(score.getText()).thenReturn("Raid Level: " + points + " <col=ffffff>(" + enabled.size() + ")</col>");
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

	@Test public void leaderHoverWhiteIsReadFromButtonCallback()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH);
		when(grid.children[Invocation.WALK_THE_PATH.uiIndex * 3 + 2].getTextColor()).thenReturn(0xFFFFFF);
		when(grid.children[Invocation.PATHMASTER.uiIndex * 3 + 2].getTextColor()).thenReturn(0xFFFFFF);
		InvocationSelection result = InvocationReader.read(grid.client);
		assertNotNull(result);
		assertEquals(EnumSet.of(Invocation.WALK_THE_PATH), result.active);
	}

	@Test public void unrecognizedCallbackScriptDoesNotSupplySelectionState()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH);
		when(grid.children[Invocation.WALK_THE_PATH.uiIndex * 3].getOnOpListener())
			.thenReturn(new Object[]{6729, 1, 0, 0});
		InvocationSelection result = InvocationReader.read(grid.client);
		assertNotNull(result);
		assertTrue(result.active.contains(Invocation.WALK_THE_PATH));
	}

	@Test public void malformedCallbackUsesRecognizedColorFallback()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH);
		Object[][] malformed = {null, {}, {6754}, {6754, 1, 0}, {6754, 1, 0, "1"}, {6754, 1, 0, 2}, {null, 1, 0, 0}};
		for (Object[] args : malformed)
		{
			when(grid.children[Invocation.WALK_THE_PATH.uiIndex * 3].getOnOpListener()).thenReturn(args);
			InvocationSelection result = InvocationReader.read(grid.client);
			assertNotNull(result);
			assertTrue(result.active.contains(Invocation.WALK_THE_PATH));
		}
	}

	@Test public void unknownColorWithoutValidCallbackRejectsEvenZeroSelection()
	{
		Grid grid = new Grid(false);
		for (int color : new int[]{0xFFFFFF, 0xD39034, 0x808080, 0})
		{
			when(grid.children[Invocation.WALK_THE_PATH.uiIndex * 3 + 2].getTextColor()).thenReturn(color);
			assertNull(InvocationReader.read(grid.client));
		}
	}

	@Test public void malformedCallbackCannotMakeUnknownColorReadable()
	{
		Grid grid = new Grid(true, Invocation.WALK_THE_PATH);
		when(grid.children[Invocation.WALK_THE_PATH.uiIndex * 3 + 2].getTextColor()).thenReturn(0xFFFFFF);
		when(grid.children[Invocation.WALK_THE_PATH.uiIndex * 3].getOnOpListener())
			.thenReturn(new Object[]{6754, 1, 0, 2});
		assertNull(InvocationReader.read(grid.client));
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
		when(grid.score.getText()).thenReturn("50");
		assertNotNull(InvocationReader.read(grid.client));
	}

	@Test public void fullHeaderValidatesColorCaptureWhileSidebarIsHidden()
	{
		Grid grid = new Grid(false, Invocation.WALK_THE_PATH);
		Widget sidebarScore = mock(Widget.class);
		when(sidebarScore.isHidden()).thenReturn(true);
		when(sidebarScore.getText()).thenReturn("300");
		when(grid.client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL_INVOCATIONS)).thenReturn(sidebarScore);
		when(grid.score.getText()).thenReturn("Raid Level: 50 <col=ffffff>(1)</col>");
		InvocationSelection result = InvocationReader.read(grid.client);
		assertNotNull(result);
		assertEquals(50, result.raidPoints);
	}

	@Test public void malformedOrHiddenPartyHeaderCannotValidateColorCapture()
	{
		Grid grid = new Grid(false, Invocation.WALK_THE_PATH);
		for (String text : new String[]{"Raid Level: 50 (bad)", "Raid Level: 50 (-1)", "Raid Level: 50 (1) extra",
			"Raid Level: -50 (1)", "Raid Level: 9999999999999999999 (1)"})
		{
			when(grid.score.getText()).thenReturn(text);
			assertNull(InvocationReader.read(grid.client));
		}
		when(grid.score.getText()).thenReturn("Raid Level: 50 <col=ffffff>(1)</col>");
		when(grid.score.isHidden()).thenReturn(true);
		assertNull(InvocationReader.read(grid.client));
	}

	@Test public void mismatchingScoreRejectsBothLeaderAndNonleaderCapture()
	{
		for (boolean leader : new boolean[]{true, false})
		{
			Grid grid = new Grid(leader, Invocation.WALK_THE_PATH);
			when(grid.score.getText()).thenReturn("Raid Level: 300 <col=ffffff>(1)</col>");
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
		for (String value : new String[]{null, "", "4 / 6", "4 (2)", "Raid Level: 4 (2)",
			"not a level", "-1", "9999999999999999999"})
		{
			when(widget.getText()).thenReturn(value);
			assertNull(InvocationReader.number(widget));
		}
		assertNull(InvocationReader.number(null));
	}
}
