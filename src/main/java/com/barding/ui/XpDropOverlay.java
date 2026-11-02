package com.barding.ui;

import com.barding.data.InstrumentDef;
import com.barding.progress.XpDrops;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

/**
 * Floating experience drops in the style of the game's own, which plugins can't trigger for a
 * skill the game doesn't have. Each drop rises up the right of the viewport and disappears.
 */
@Singleton
public class XpDropOverlay extends Overlay
{
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 3;
	private static final int PART_GAP = 10;
	private static final int START_Y = 150;
	private static final int END_Y = 40;
	private static final int RIGHT_PAD_FIXED = 10;
	private static final int RIGHT_PAD_RESIZED = 190; // clears the minimap

	private final Client client;
	private final XpDrops drops;
	private final InstrumentIcons icons;
	private final Map<String, BufferedImage> smallIcons = new HashMap<>();
	private BufferedImage bardingIcon;

	@Inject
	public XpDropOverlay(Client client, XpDrops drops, InstrumentIcons icons)
	{
		this.client = client;
		this.drops = drops;
		this.icons = icons;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		long now = System.currentTimeMillis();
		List<XpDrops.Drop> active = drops.active(now);
		if (active.isEmpty())
		{
			return null;
		}
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		int pad = client.isResized() ? RIGHT_PAD_RESIZED : RIGHT_PAD_FIXED;
		int right = client.getViewportXOffset() + client.getViewportWidth() - pad;
		int top = client.getViewportYOffset();
		for (XpDrops.Drop drop : active)
		{
			double progress = Math.min(1, (now - drop.getStartedAt()) / (double) XpDrops.LIFETIME);
			int y = top + START_Y - (int) ((START_Y - END_Y) * progress);
			drawDrop(g, fm, drop, right, y);
		}
		return null;
	}

	private void drawDrop(Graphics2D g, FontMetrics fm, XpDrops.Drop drop, int right, int y)
	{
		int width = -PART_GAP;
		for (XpDrops.Part part : drop.getParts())
		{
			width += ICON_SIZE + ICON_GAP + fm.stringWidth(amount(part)) + PART_GAP;
		}
		int x = right - width;
		for (XpDrops.Part part : drop.getParts())
		{
			BufferedImage icon = iconFor(part.getInstrument());
			g.drawImage(icon, x + (ICON_SIZE - icon.getWidth()) / 2, y - ICON_SIZE + 3 + (ICON_SIZE - icon.getHeight()) / 2, null);
			x += ICON_SIZE + ICON_GAP;
			String text = amount(part);
			g.setColor(Color.BLACK);
			g.drawString(text, x + 1, y + 1);
			g.setColor(Color.WHITE);
			g.drawString(text, x, y);
			x += fm.stringWidth(text) + PART_GAP;
		}
	}

	private static String amount(XpDrops.Part part)
	{
		return QuantityFormatter.formatNumber(part.getAmount());
	}

	private BufferedImage iconFor(InstrumentDef instrument)
	{
		if (instrument == null)
		{
			if (bardingIcon == null)
			{
				bardingIcon = icons.skillIcon(ICON_SIZE);
			}
			return bardingIcon;
		}
		return smallIcons.computeIfAbsent(instrument.getId(),
			id -> ImageUtil.resizeImage(icons.get(instrument), ICON_SIZE, ICON_SIZE, true));
	}
}
