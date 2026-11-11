package com.cornwall92.toapathwatch;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** A complete observed selection. null at the reader boundary means unknown, not empty. */
public final class InvocationSelection
{
	public final Set<Invocation> active;
	public final int raidPoints;

	private InvocationSelection(Set<Invocation> active, int raidPoints)
	{
		this.active = Collections.unmodifiableSet(active);
		this.raidPoints = raidPoints;
	}

	public static InvocationSelection validate(Map<Invocation, Boolean> states, Integer displayedPoints)
	{
		if (states.size() != Invocation.values().length || states.containsValue(null))
		{
			return null;
		}
		EnumSet<Invocation> active = EnumSet.noneOf(Invocation.class);
		int sum = 0;
		for (Invocation inv : Invocation.values())
		{
			if (Boolean.TRUE.equals(states.get(inv)))
			{
				active.add(inv);
				sum += inv.raidPoints;
			}
		}
		if (displayedPoints != null && displayedPoints != sum)
		{
			return null;
		}
		return new InvocationSelection(active, sum);
	}
}
