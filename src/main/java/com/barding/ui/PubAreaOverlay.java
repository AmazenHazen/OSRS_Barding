package com.barding.ui;

import com.barding.data.PubDef;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** A debugging aid: highlights every tile that counts as a pub. Only added while it is switched on. */
@Singleton
public class PubAreaOverlay extends Overlay
{
	private static final Color FILL = new Color(0, 255, 0, 50);
	private static final Color EDGE = new Color(0, 255, 0, 150);

	private final Client client;
	private List<PubDef> pubs = new ArrayList<>();

	@Inject
	public PubAreaOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void setPubs(List<PubDef> pubs)
	{
		this.pubs = pubs;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		WorldView world = client.getTopLevelWorldView();
		g.setFont(FontManager.getRunescapeBoldFont());
		for (PubDef pub : pubs)
		{
			if (pub.getPlane() == world.getPlane())
			{
				draw(g, world, pub);
			}
		}
		return null;
	}

	private void draw(Graphics2D g, WorldView world, PubDef pub)
	{
		int centreX = (pub.getMinX() + pub.getMaxX()) / 2;
		int centreY = (pub.getMinY() + pub.getMaxY()) / 2;
		for (int x = pub.getMinX(); x <= pub.getMaxX(); x++)
		{
			for (int y = pub.getMinY(); y <= pub.getMaxY(); y++)
			{
				LocalPoint tile = LocalPoint.fromWorld(world, x, y);
				Polygon polygon = tile == null ? null : Perspective.getCanvasTilePoly(client, tile);
				if (polygon == null)
				{
					continue;
				}
				g.setColor(FILL);
				g.fill(polygon);
				g.setColor(EDGE);
				g.draw(polygon);
				if (x == centreX && y == centreY)
				{
					drawName(g, polygon, pub.getName());
				}
			}
		}
	}

	private static void drawName(Graphics2D g, Polygon polygon, String name)
	{
		Rectangle bounds = polygon.getBounds();
		int x = bounds.x + bounds.width / 2 - g.getFontMetrics().stringWidth(name) / 2;
		g.setColor(Color.BLACK);
		g.drawString(name, x + 1, bounds.y + 1);
		g.setColor(Color.GREEN);
		g.drawString(name, x, bounds.y);
	}
}
