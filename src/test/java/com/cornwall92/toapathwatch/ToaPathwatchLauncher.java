package com.cornwall92.toapathwatch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ToaPathwatchLauncher
{
	// RuneLite exposes this registration method as generic Class<? extends Plugin> varargs.
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ToaPathwatchPlugin.class);
		RuneLite.main(args);
	}
}
