package com.cornwall92.toapathwatch;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import static net.runelite.api.Constants.CHUNK_SIZE;
import static net.runelite.api.Constants.SCENE_SIZE;

/** Resolves a template region without treating an unloaded instance chunk as an exit. */
public final class RegionResolver
{
	private RegionResolver() {}

	/** Must be called on the client thread. -1 means the location is not yet known. */
	public static int region(Client client)
	{
		Player player = client.getLocalPlayer();
		if (player == null) return -1;
		LocalPoint local = player.getLocalLocation();
		if (local == null) return -1;
		WorldView worldView = client.getWorldView(local.getWorldView());
		if (worldView == null) return -1;

		if (!worldView.isInstance())
		{
			return WorldPoint.fromLocal(worldView, local.getX(), local.getY(), worldView.getPlane()).getRegionID();
		}

		int sceneX = local.getSceneX();
		int sceneY = local.getSceneY();
		if (sceneX < 0 || sceneY < 0 || sceneX >= SCENE_SIZE || sceneY >= SCENE_SIZE) return -1;
		int plane = worldView.getPlane();
		int chunkX = sceneX / CHUNK_SIZE;
		int chunkY = sceneY / CHUNK_SIZE;
		int[][][] chunks = worldView.getInstanceTemplateChunks();
		if (chunks == null || plane < 0 || plane >= chunks.length || chunks[plane] == null
			|| chunkX >= chunks[plane].length || chunks[plane][chunkX] == null
			|| chunkY >= chunks[plane][chunkX].length || chunks[plane][chunkX][chunkY] == -1)
		{
			return -1;
		}
		// RuneLite's conversion assumes the chunk is loaded: -1 otherwise decodes to
		// a synthetic region rather than an unknown result, prematurely clearing a raid.
		return WorldPoint.fromLocalInstance(client, local).getRegionID();
	}
}
