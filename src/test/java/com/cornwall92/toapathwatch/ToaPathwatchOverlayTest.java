package com.cornwall92.toapathwatch;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.config.FontType;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Uses RuneLite's real layout components; no game client or input automation. */
public class ToaPathwatchOverlayTest
{
	private Client client;
	private ToaPathwatchPlugin plugin;
	private ToaPathwatchConfig config;
	private RuneLiteConfig runeLiteConfig;
	private TooltipManager tooltips;
	private ToaPathwatchOverlay overlay;
	private Graphics2D graphics;

	@Before public void setUp()
	{
		client = mock(Client.class);
		plugin = mock(ToaPathwatchPlugin.class);
		config = mock(ToaPathwatchConfig.class, CALLS_REAL_METHODS);
		runeLiteConfig = mock(RuneLiteConfig.class);
		when(runeLiteConfig.tooltipFont()).thenReturn(FontType.REGULAR.withFamily(Font.DIALOG).withSize(12));
		tooltips = new TooltipManager();
		overlay = new ToaPathwatchOverlay(plugin, config, client, tooltips, runeLiteConfig);
		// Keep the rendered children available for locating the actual displayed rows.
		overlay.setClearChildren(false);
		overlay.getBounds().setLocation(80, 100);
		graphics = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_ARGB).createGraphics();
		graphics.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		when(client.getMouseCanvasPosition()).thenReturn(new Point(-1, -1));
		when(plugin.getBoss()).thenReturn(Boss.KEPHRI);
		when(plugin.getPathLevel()).thenReturn(1);
		select(Invocation.WALK_THE_PATH, Invocation.LIVELY_LARVAE);
	}

	@After public void tearDown()
	{
		graphics.dispose();
	}

	private void select(Invocation... active)
	{
		EnumSet<Invocation> enabled = EnumSet.noneOf(Invocation.class);
		for (Invocation invocation : active) enabled.add(invocation);
		Map<Invocation, Boolean> states = new EnumMap<>(Invocation.class);
		for (Invocation invocation : Invocation.values()) states.put(invocation, enabled.contains(invocation));
		when(plugin.getSelection()).thenReturn(InvocationSelection.validate(states, null));
	}

	private Dimension render()
	{
		// These are normally cleared by OverlayPanel and TooltipOverlay each frame.
		overlay.getPanelComponent().getChildren().clear();
		tooltips.clear();
		return overlay.render(graphics);
	}

	private Rectangle row(int index)
	{
		return new Rectangle(overlay.getPanelComponent().getChildren().get(index).getBounds());
	}

	private void hover(Rectangle row)
	{
		when(client.getMouseCanvasPosition()).thenReturn(new Point(
			overlay.getBounds().x + row.x + 2,
			overlay.getBounds().y + row.y + row.height - 1));
	}

	private String tooltip()
	{
		assertEquals(1, tooltips.getTooltips().size());
		return tooltips.getTooltips().get(0).getText();
	}

	private void assertTooltipFor(Invocation invocation)
	{
		String text = tooltip();
		assertTrue(text.startsWith("<col=ffc355>" + invocation.displayName + "</col><br>"));
		assertEquals(invocation.description,
			text.substring(text.indexOf("<br>") + 4).replace("<br>", " "));
	}

	private void assertTooltipFitsConfiguredFont()
	{
		FontMetrics metrics = graphics.getFontMetrics(runeLiteConfig.tooltipFont().getFont());
		String[] lines = tooltip().split("<br>");
		assertTrue(lines.length > 2);
		for (int i = 1; i < lines.length; i++)
		{
			assertTrue("Tooltip line exceeds 300 pixels: " + lines[i], metrics.stringWidth(lines[i]) <= 300);
		}
	}

	@Test public void defaultsShowNamesAndHoverDescriptions()
	{
		assertTrue(config.showInvocations());
		assertTrue(config.invocationTooltips());
		render();
		hover(row(3));
		render();
		assertTooltipFor(Invocation.LIVELY_LARVAE);
	}

	@Test public void hoverFollowsMovedOverlayWithoutUsingOldCanvasCoordinates()
	{
		render();
		Rectangle invocationRow = row(2);
		hover(invocationRow);
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);

		overlay.getBounds().setLocation(600, 400);
		render();
		assertTrue(tooltips.getTooltips().isEmpty());
		hover(invocationRow);
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);
	}

	@Test public void wrappedNameAndPointsRemainHoverableOnTheirLastLine()
	{
		when(plugin.getBoss()).thenReturn(Boss.AKKHA);
		select(Invocation.FEELING_SPECIAL);
		when(config.width()).thenReturn(180);
		when(config.showPoints()).thenReturn(true);
		graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
		render();
		Rectangle invocationRow = row(2);
		assertTrue(invocationRow.height > graphics.getFontMetrics().getHeight());
		hover(invocationRow);
		render();
		assertTooltipFor(Invocation.FEELING_SPECIAL);
		assertTooltipFitsConfiguredFont();
	}

	@Test public void descriptionWrappingUsesTooltipFontRatherThanSmallerOverlayFont()
	{
		graphics.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
		when(runeLiteConfig.tooltipFont()).thenReturn(FontType.REGULAR.withFamily(Font.MONOSPACED).withSize(24));
		assertTrue(graphics.getFontMetrics(runeLiteConfig.tooltipFont().getFont()).getHeight()
			> graphics.getFontMetrics().getHeight());
		render();
		hover(row(2));
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);
		assertTooltipFitsConfiguredFont();
	}

	@Test public void hideToggleRemovesWholeSectionAndKeepsPathEffects()
	{
		render();
		hover(row(2));
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);
		assertEquals(7, overlay.getPanelComponent().getChildren().size());

		when(config.showInvocations()).thenReturn(false);
		clearInvocations(plugin);
		assertNotNull(render());
		assertEquals(4, overlay.getPanelComponent().getChildren().size());
		verify(plugin, never()).getSelection();
		assertTrue(tooltips.getTooltips().isEmpty());

		when(config.showInvocations()).thenReturn(true);
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);
	}

	@Test public void disablingDescriptionsKeepsNamesAndTakesEffectImmediately()
	{
		render();
		hover(row(2));
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);
		int visibleRows = overlay.getPanelComponent().getChildren().size();

		when(config.invocationTooltips()).thenReturn(false);
		render();
		assertEquals(visibleRows, overlay.getPanelComponent().getChildren().size());
		assertTrue(tooltips.getTooltips().isEmpty());
		when(config.invocationTooltips()).thenReturn(true);
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);
	}

	@Test public void titleHeadingsAndOutsidePanelHaveNoInvocationTooltip()
	{
		render();
		for (int index : new int[]{0, 1, 4})
		{
			hover(row(index));
			render();
			assertTrue(tooltips.getTooltips().isEmpty());
		}
		when(client.getMouseCanvasPosition()).thenReturn(new Point(-1, -1));
		render();
		assertTrue(tooltips.getTooltips().isEmpty());
	}

	@Test public void openContextMenuSuppressesTooltip()
	{
		render();
		hover(row(2));
		when(client.isMenuOpen()).thenReturn(true);
		render();
		assertTrue(tooltips.getTooltips().isEmpty());
	}

	@Test public void changedSelectionAndBossUseOnlyCurrentVisibleRows()
	{
		select(Invocation.LIVELY_LARVAE, Invocation.UPSET_STOMACH);
		render();
		hover(row(2));
		render();
		assertTooltipFor(Invocation.LIVELY_LARVAE);

		when(plugin.getBoss()).thenReturn(Boss.ZEBAK);
		render();
		assertTooltipFor(Invocation.UPSET_STOMACH);

		select();
		render();
		assertTrue(tooltips.getTooltips().isEmpty());
		when(plugin.getSelection()).thenReturn(null);
		render();
		assertTrue(tooltips.getTooltips().isEmpty());
	}

	@Test public void leavingBossClearsHoverTargetsForLobbyAndHiddenPanel()
	{
		render();
		hover(row(2));
		render();
		assertTooltipFor(Invocation.WALK_THE_PATH);

		when(plugin.getBoss()).thenReturn(null);
		when(plugin.getLobbyStatus()).thenReturn("Captured 2 invocations. Raid level: 55.");
		assertNotNull(render());
		assertEquals(2, overlay.getPanelComponent().getChildren().size());
		assertTrue(tooltips.getTooltips().isEmpty());
		when(plugin.getLobbyStatus()).thenReturn(null);
		assertNull(render());
		assertTrue(tooltips.getTooltips().isEmpty());
	}
}
