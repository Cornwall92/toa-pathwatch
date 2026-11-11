package com.cornwall92.toapathwatch;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RegionResolverTest
{
	private Client client;
	private Player player;
	private WorldView worldView;

	@Before public void setUp()
	{
		client = mock(Client.class);
		player = mock(Player.class);
		worldView = mock(WorldView.class);
		when(worldView.getId()).thenReturn(WorldView.TOPLEVEL);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(worldView);
		when(player.getLocalLocation()).thenReturn(at(12, 12));
	}

	private LocalPoint at(int sceneX, int sceneY)
	{
		return new LocalPoint(sceneX * 128 + 64, sceneY * 128 + 64, WorldView.TOPLEVEL);
	}

	private int[][][] instance()
	{
		int[][][] chunks = new int[4][13][13];
		when(worldView.isInstance()).thenReturn(true);
		when(worldView.getInstanceTemplateChunks()).thenReturn(chunks);
		return chunks;
	}

	@Test public void missingPlayerIsUnknown()
	{
		when(client.getLocalPlayer()).thenReturn(null);
		assertEquals(-1, RegionResolver.region(client));
	}

	@Test public void missingLocalPointIsUnknown()
	{
		when(player.getLocalLocation()).thenReturn(null);
		assertEquals(-1, RegionResolver.region(client));
	}

	@Test public void missingWorldViewIsUnknown()
	{
		when(client.getWorldView(WorldView.TOPLEVEL)).thenReturn(null);
		assertEquals(-1, RegionResolver.region(client));
	}

	@Test public void ordinaryWorldUsesPlayersWorldView()
	{
		when(worldView.getBaseX()).thenReturn(52 * 64);
		when(worldView.getBaseY()).thenReturn(142 * 64);
		assertEquals(13454, RegionResolver.region(client));
		verify(client, never()).getTopLevelWorldView();
	}

	@Test public void validInstanceUsesTemplateRegion()
	{
		int[][][] chunks = instance();
		chunks[0][1][1] = (57 * 8 << 14) | (84 * 8 << 3);
		assertEquals(Boss.AKKHA.bossRegion, RegionResolver.region(client));
	}

	@Test public void unloadedChunkIsUnknownInsteadOfSyntheticOutsideRegion()
	{
		int[][][] chunks = instance();
		chunks[0][1][1] = -1;
		assertEquals(-1, RegionResolver.region(client));
	}

	@Test public void missingInstanceArrayIsUnknown()
	{
		when(worldView.isInstance()).thenReturn(true);
		assertEquals(-1, RegionResolver.region(client));
	}

	@Test public void invalidOrUnloadedPlaneIsUnknown()
	{
		int[][][] chunks = instance();
		when(worldView.getPlane()).thenReturn(-1);
		assertEquals(-1, RegionResolver.region(client));
		when(worldView.getPlane()).thenReturn(4);
		assertEquals(-1, RegionResolver.region(client));
		when(worldView.getPlane()).thenReturn(0);
		chunks[0] = null;
		assertEquals(-1, RegionResolver.region(client));
	}

	@Test public void missingOrShortChunkRowsAreUnknown()
	{
		int[][][] chunks = instance();
		chunks[0][1] = null;
		assertEquals(-1, RegionResolver.region(client));
		chunks[0][1] = new int[1];
		assertEquals(-1, RegionResolver.region(client));
		chunks[0] = new int[1][13];
		assertEquals(-1, RegionResolver.region(client));
	}

	@Test public void sceneCoordinatesOutsideTemplateGridAreUnknown()
	{
		instance();
		when(player.getLocalLocation()).thenReturn(at(-1, 12));
		assertEquals(-1, RegionResolver.region(client));
		when(player.getLocalLocation()).thenReturn(at(12, -1));
		assertEquals(-1, RegionResolver.region(client));
		when(player.getLocalLocation()).thenReturn(at(104, 12));
		assertEquals(-1, RegionResolver.region(client));
		when(player.getLocalLocation()).thenReturn(at(12, 104));
		assertEquals(-1, RegionResolver.region(client));
	}
}
