package com.cornwall92.toapathwatch;

import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Exercises the HUD number parser; party-selection capture is covered by PartyBuildReaderTest. */
public class InvocationReaderTest
{
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
