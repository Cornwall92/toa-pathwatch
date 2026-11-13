package com.cornwall92.toapathwatch;

import java.util.EnumMap;
import java.util.Map;

public final class InvocationReader
{
	static final int PARTY_BUILD_SCRIPT = 6729;

	private InvocationReader() {}

	/**
	 * Script 6729's input is the complete selection used to build the party screen.
	 * Read before the script runs: post-script widgets can be recoloured by other plugins.
	 * Arguments include the script ID, role, party metadata, count, score, tab and three
	 * 31-bit selection words. Roles 1/2 are a member/leader of the current party.
	 */
	public static InvocationSelection readPartyBuild(Object[] args)
	{
		if (args == null || args.length != 9) return null;
		for (Object arg : args) if (!(arg instanceof Integer)) return null;
		if ((Integer) args[0] != PARTY_BUILD_SCRIPT) return null;
		int role = (Integer) args[1];
		if (role != 1 && role != 2) return null;
		int tab = (Integer) args[5];
		if (tab < 0 || tab > 3) return null;
		int[] words = {(Integer) args[6], (Integer) args[7], (Integer) args[8]};
		int[] knownBits = new int[3];
		Map<Invocation, Boolean> states = new EnumMap<>(Invocation.class);
		int count = 0;
		for (Invocation invocation : Invocation.values())
		{
			// Cache param 1159: ordinary invocation bits are 1..44 in UI order.
			// Script 6755 splits at 31 and 62, not at 32 and 64.
			int bit = invocation.uiIndex + 1;
			int word = bit / 31;
			int mask = 1 << (bit % 31);
			knownBits[word] |= mask;
			boolean active = (words[word] & mask) != 0;
			states.put(invocation, active);
			if (active) count++;
		}
		for (int i = 0; i < words.length; i++)
		{
			// Do not silently drop event/league-only or future invocations.
			if ((words[i] & ~knownBits[i]) != 0) return null;
		}
		if (count != (Integer) args[3]) return null;
		return InvocationSelection.validate(states, (Integer) args[4]);
	}
}
