package com.cornwall92.toapathwatch;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

public final class InvocationReader
{
	// Observed by the ToA Invocation Display author for non-leaders. See RESEARCH.md.
	private static final int ACTIVE_LABEL_COLOR = 0xD39034;
	private static final Pattern SINGLE_NUMBER = Pattern.compile("^(?:Raid [Ll]evel:?\\s*)?(\\d+)$");

	private InvocationReader() {}

	public static InvocationSelection read(Client client)
	{
		Widget parent = client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT);
		if (parent == null || parent.isHidden() || parent.getChildren() == null)
		{
			return null;
		}
		Widget[] children = parent.getChildren();
		Map<Invocation, Boolean> states = new EnumMap<>(Invocation.class);
		boolean usedColor = false;
		for (int i = 2; i < children.length; i++)
		{
			Widget label = children[i];
			Invocation invocation = label == null ? null : Invocation.fromName(label.getText());
			if (invocation == null)
			{
				continue;
			}
			if (states.containsKey(invocation))
			{
				return null; // Ambiguous or partially rebuilt layout.
			}
			Widget background = children[i - 2];
			Object[] args = background == null ? null : background.getOnOpListener();
			Boolean active = null;
			if (args != null && args.length >= 4)
			{
				if (Integer.valueOf(1).equals(args[3])) active = true;
				else if (Integer.valueOf(0).equals(args[3])) active = false;
			}
			if (active == null)
			{
				usedColor = true;
				active = label.getTextColor() == ACTIVE_LABEL_COLOR;
			}
			states.put(invocation, active);
		}
		Integer points = number(client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL));
		if (points == null)
		{
			points = number(client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL_INVOCATIONS));
		}
		// Color reads need a matching score so a default-grey loading frame cannot erase selections.
		// A real zero-invocation raid with a displayed score of zero is valid.
		if (usedColor && points == null)
		{
			return null;
		}
		return InvocationSelection.validate(states, points);
	}

	public static Integer number(Widget widget)
	{
		if (widget == null || widget.isHidden()) return null;
		String text = widget.getText();
		if (text == null) return null;
		Matcher matcher = SINGLE_NUMBER.matcher(text.replaceAll("<[^>]*>", "").trim());
		if (!matcher.matches()) return null;
		try { return Integer.valueOf(matcher.group(1)); }
		catch (NumberFormatException ex) { return null; }
	}
}
