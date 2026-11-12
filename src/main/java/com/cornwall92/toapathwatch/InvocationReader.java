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
	// CS2 scripts 6750/6751: label colours and the callback carrying leader selection state.
	private static final int ACTIVE_LABEL_COLOR = 0xD39F34;
	private static final int INACTIVE_LABEL_COLOR = 0x9F9F9F;
	private static final Pattern SINGLE_NUMBER = Pattern.compile("^(?:Raid [Ll]evel:?\\s*)?(\\d+)$");
	// Script 6729 appends the active invocation count, e.g. "Raid Level: 250 (16)".
	private static final Pattern PARTY_SCORE = Pattern.compile("^(?:Raid [Ll]evel:?\\s*)?(\\d+)(?:\\s+\\(\\d+\\))?$");

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
			Boolean active = background == null ? null : listenerState(background.getOnOpListener(), 6754, 3);
			if (active == null)
			{
				// Non-leader labels keep their state colours on hover (scripts 6752/6753).
				// Leader labels can turn white, but their 6754 callback above retains the state.
				usedColor = true;
				int color = label.getTextColor();
				if (color == ACTIVE_LABEL_COLOR) active = true;
				else if (color == INACTIVE_LABEL_COLOR) active = false;
				else return null; // Unknown/hover styling is not an inactive selection.
			}
			states.put(invocation, active);
		}
		Integer points = readNumber(client.getWidget(InterfaceID.ToaPartydetails.RAID_LEVEL), PARTY_SCORE);
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

	private static Boolean listenerState(Object[] args, int scriptId, int stateIndex)
	{
		if (args == null || args.length <= stateIndex || !Integer.valueOf(scriptId).equals(args[0])) return null;
		if (Integer.valueOf(1).equals(args[stateIndex])) return true;
		if (Integer.valueOf(0).equals(args[stateIndex])) return false;
		return null;
	}

	public static Integer number(Widget widget)
	{
		return readNumber(widget, SINGLE_NUMBER);
	}

	private static Integer readNumber(Widget widget, Pattern pattern)
	{
		if (widget == null || widget.isHidden()) return null;
		String text = widget.getText();
		if (text == null) return null;
		Matcher matcher = pattern.matcher(text.replaceAll("<[^>]*>", "").trim());
		if (!matcher.matches()) return null;
		try { return Integer.valueOf(matcher.group(1)); }
		catch (NumberFormatException ex) { return null; }
	}
}
