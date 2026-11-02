package com.barding.ui;

import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.barding.progress.InstrumentInstance;
import com.barding.progress.InstrumentRank;
import com.barding.progress.InstrumentSkills;
import com.barding.progress.InstrumentStorage;
import com.barding.progress.TuningService;
import com.barding.progress.TuningState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.QuantityFormatter;

/**
 * Adds subtabs to the top of the music tab: Music, Storage and Levels. The game's music list is moved
 * down to make room, and hidden while either of our subtabs is taking over the tab.
 */
@Singleton
public class InstrumentTab extends Overlay implements MouseWheelListener
{
	private enum View
	{
		MUSIC("Music", 0),
		STORAGE("Storage", 34),
		LEVELS("Levels", 46);

		final String label;
		final int rowHeight;

		View(String label, int rowHeight)
		{
			this.label = label;
			this.rowHeight = rowHeight;
		}
	}

	private static final int STRIP_HEIGHT = 20;
	private static final int ROW_ICON_X = 3;
	private static final int ROW_TEXT_X = 35;
	private static final int ROW_RIGHT_PAD = 5;
	private static final int BAR_Y = 31;
	private static final int BAR_HEIGHT = 9;
	private static final int SCROLLBAR_WIDTH = 6;

	private static final Color BACKGROUND = new Color(0x3E3529);
	private static final Color ROW_DARK = new Color(0x393228);
	private static final Color ROW_HOVER = new Color(0x4F4537);
	private static final Color TAB = new Color(0x3F362B);
	private static final Color TAB_SELECTED = new Color(0x726451);
	private static final Color BORDER = new Color(0x726451);
	private static final Color SCROLL_TRACK = new Color(0x0E0E0C);
	private static final Color BAR_EMPTY = new Color(0x2E2B23);
	private static final Color ORANGE = new Color(0xFF981F);
	private static final Color GOLD = new Color(0xFFD700);
	private static final Color GREY = new Color(0xB0A890);

	private final Client client;
	private final InstrumentStorage storage;
	private final InstrumentSkills skills;
	private final TuningService tuningService;
	private final InstrumentIcons icons;
	private final OpenMenus menus;
	private final TooltipManager tooltipManager;
	private InstrumentRepository instruments;

	// The music list's size before we moved it
	private boolean moved;
	private int originalY;
	private int originalHeight;

	private volatile View view = View.MUSIC;
	private volatile int scroll;
	private volatile Rectangle listRect = new Rectangle();

	// Hit boxes, rebuilt every frame by render()
	private final Rectangle[] tabRects = new Rectangle[View.values().length];
	private List<Row> rows = new ArrayList<>();

	/** One row of a list. The instance is only set in Storage. */
	@Value
	private static class Row
	{
		Rectangle bounds;
		InstrumentDef def;
		InstrumentInstance instance;
	}

	@Inject
	public InstrumentTab(Client client, InstrumentStorage storage, InstrumentSkills skills, TuningService tuningService,
		InstrumentIcons icons, OpenMenus menus, TooltipManager tooltipManager)
	{
		this.client = client;
		this.storage = storage;
		this.skills = skills;
		this.tuningService = tuningService;
		this.icons = icons;
		this.menus = menus;
		this.tooltipManager = tooltipManager;
		for (int i = 0; i < tabRects.length; i++)
		{
			tabRects[i] = new Rectangle();
		}
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	public void setInstruments(InstrumentRepository instruments)
	{
		this.instruments = instruments;
	}

	/** Call every client tick. Cheap: only touches the music tab if the game has reset it. */
	public void update()
	{
		Widget contents = client.getWidget(InterfaceID.Music.CONTENTS);
		if (contents == null)
		{
			return;
		}
		if (contents.getOriginalY() != STRIP_HEIGHT)
		{
			originalY = contents.getOriginalY();
			originalHeight = contents.getOriginalHeight();
			moved = true;
			contents.setOriginalY(STRIP_HEIGHT);
			contents.setOriginalHeight(shrunkHeight(contents));
			contents.revalidate();
		}
		boolean hide = view != View.MUSIC;
		if (contents.isSelfHidden() != hide)
		{
			contents.setHidden(hide);
		}
	}

	/** A height of "the parent's height minus N" has to grow N, not shrink, to end up shorter. */
	private int shrunkHeight(Widget contents)
	{
		boolean relative = contents.getHeightMode() == WidgetSizeMode.MINUS;
		return originalHeight + (relative ? STRIP_HEIGHT : -STRIP_HEIGHT);
	}

	/** Undo everything. Call on the client thread when the plugin shuts down. */
	public void reset()
	{
		view = View.MUSIC;
		Widget contents = client.getWidget(InterfaceID.Music.CONTENTS);
		if (contents != null && moved)
		{
			contents.setOriginalY(originalY);
			contents.setOriginalHeight(originalHeight);
			contents.setHidden(false);
			contents.revalidate();
		}
		moved = false;
	}

	/** Call once per frame while the right-click menu is being built. */
	public void addMenuOptions(Point mouse)
	{
		if (!isOpen())
		{
			return;
		}
		for (View tab : View.values())
		{
			if (tabRects[tab.ordinal()].contains(mouse.getX(), mouse.getY()))
			{
				menus.addOption("View", tab.label, () -> show(tab));
				return;
			}
		}
		if (view != View.STORAGE || !listRect.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}
		for (Row row : rows)
		{
			if (row.getBounds().contains(mouse.getX(), mouse.getY()))
			{
				addStorageOptions(row);
				return;
			}
		}
	}

	private void addStorageOptions(Row row)
	{
		String id = row.getDef().getId();
		String name = row.getDef().getName();
		if (row.getInstance().isCarried())
		{
			menus.addOption("Store", name, () ->
			{
				tuningService.stop();
				storage.store(id);
			});
		}
		else
		{
			menus.addOption("Take", name, () -> storage.take(id));
		}
	}

	private void show(View newView)
	{
		view = newView;
		scroll = 0;
	}

	private boolean isOpen()
	{
		Widget universe = client.getWidget(InterfaceID.Music.UNIVERSE);
		return universe != null && !universe.isHidden();
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		rows = new ArrayList<>();
		Widget universe = client.getWidget(InterfaceID.Music.UNIVERSE);
		if (universe == null || universe.isHidden() || instruments == null)
		{
			return null;
		}

		Rectangle bounds = universe.getBounds();
		g.setFont(FontManager.getRunescapeSmallFont());
		View[] tabs = View.values();
		for (int i = 0; i < tabs.length; i++)
		{
			int left = bounds.x + bounds.width * i / tabs.length;
			int right = bounds.x + bounds.width * (i + 1) / tabs.length;
			tabRects[i] = new Rectangle(left, bounds.y, right - left, STRIP_HEIGHT);
			drawTab(g, tabRects[i], tabs[i].label, tabs[i] == view);
		}

		Rectangle list = new Rectangle(bounds.x, bounds.y + STRIP_HEIGHT, bounds.width, bounds.height - STRIP_HEIGHT);
		listRect = list;
		if (view != View.MUSIC)
		{
			g.setColor(BACKGROUND);
			g.fill(list);
			drawList(g, list, view == View.STORAGE ? storageRows() : levelRows());
		}
		return null;
	}

	private void drawTab(Graphics2D g, Rectangle r, String label, boolean selected)
	{
		g.setColor(selected ? TAB_SELECTED : TAB);
		g.fill(r);
		g.setColor(BORDER);
		g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
		int textW = g.getFontMetrics().stringWidth(label);
		drawText(g, label, r.x + (r.width - textW) / 2, r.y + 14, ORANGE);
	}

	private void drawList(Graphics2D g, Rectangle list, List<Row> sorted)
	{
		if (sorted.isEmpty())
		{
			drawEmpty(g, list);
			return;
		}

		int rowHeight = view.rowHeight;
		int contentHeight = sorted.size() * rowHeight;
		int maxScroll = Math.max(0, contentHeight - list.height);
		int s = Math.max(0, Math.min(maxScroll, scroll));
		scroll = s;
		int rowW = list.width - (maxScroll > 0 ? SCROLLBAR_WIDTH : 0);

		Shape oldClip = g.getClip();
		g.setClip(list);
		Point mouse = client.getMouseCanvasPosition();
		List<Row> visible = new ArrayList<>();
		for (int i = 0; i < sorted.size(); i++)
		{
			Row row = sorted.get(i);
			Rectangle r = new Rectangle(list.x, list.y + i * rowHeight - s, rowW, rowHeight);
			if (r.y + r.height <= list.y || r.y >= list.y + list.height)
			{
				continue;
			}
			Row placed = new Row(r, row.getDef(), row.getInstance());
			visible.add(placed);
			boolean hovered = r.contains(mouse.getX(), mouse.getY());
			g.setColor(hovered ? ROW_HOVER : i % 2 == 0 ? ROW_DARK : BACKGROUND);
			g.fill(r);
			drawIcon(g, placed);
			if (view == View.STORAGE)
			{
				drawStorageRow(g, placed);
			}
			else
			{
				drawLevelRow(g, placed);
				if (hovered)
				{
					tooltipManager.add(new Tooltip(xpTooltip(row.getDef())));
				}
			}
		}
		g.setClip(oldClip);
		rows = visible;

		if (maxScroll > 0)
		{
			int trackX = list.x + list.width - SCROLLBAR_WIDTH;
			int thumbH = Math.max(20, list.height * list.height / contentHeight);
			g.setColor(SCROLL_TRACK);
			g.fillRect(trackX, list.y, SCROLLBAR_WIDTH, list.height);
			g.setColor(BORDER);
			g.fillRect(trackX + 1, list.y + (list.height - thumbH) * s / maxScroll, SCROLLBAR_WIDTH - 2, thumbH);
		}
	}

	private void drawEmpty(Graphics2D g, Rectangle list)
	{
		drawText(g, "No instruments unlocked yet.", list.x + 8, list.y + 20, ORANGE);
		drawText(g, "Craft one to add it here.", list.x + 8, list.y + 36, GREY);
	}

	private void drawIcon(Graphics2D g, Row row)
	{
		Rectangle r = row.getBounds();
		BufferedImage icon = icons.get(row.getDef());
		g.drawImage(icon, r.x + ROW_ICON_X + (InstrumentIcons.SIZE - icon.getWidth()) / 2, r.y + (r.height - icon.getHeight()) / 2, null);
	}

	private void drawStorageRow(Graphics2D g, Row row)
	{
		Rectangle r = row.getBounds();
		InstrumentInstance instance = row.getInstance();
		drawText(g, row.getDef().getName(), r.x + ROW_TEXT_X, r.y + 14, ORANGE);
		String tuning = instance.getTuning() + "% in tune" + (instance.isCarried() ? " - carried" : "");
		drawText(g, tuning, r.x + ROW_TEXT_X, r.y + 28, tuningColor(instance.getTuning()));
	}

	private void drawLevelRow(Graphics2D g, Row row)
	{
		Rectangle r = row.getBounds();
		int xp = skills.getXp(row.getDef().getType());
		InstrumentRank rank = InstrumentRank.of(xp);
		int percent = InstrumentRank.progressPercent(xp);
		boolean expert = rank.next() == null;

		drawText(g, row.getDef().getName(), r.x + ROW_TEXT_X, r.y + 13, ORANGE);
		drawText(g, rank.getLabel(), r.x + ROW_TEXT_X, r.y + 26, expert ? GOLD : Color.WHITE);

		int right = r.x + r.width - ROW_RIGHT_PAD;
		if (!expert)
		{
			String label = percent + "%";
			drawText(g, label, right - g.getFontMetrics().stringWidth(label), r.y + 26, Color.WHITE);
		}

		int barX = r.x + ROW_TEXT_X;
		int barW = right - barX;
		g.setColor(BAR_EMPTY);
		g.fillRect(barX, r.y + BAR_Y, barW, BAR_HEIGHT);
		g.setColor(expert ? GOLD : ORANGE);
		g.fillRect(barX, r.y + BAR_Y, barW * percent / 100, BAR_HEIGHT);
		g.setColor(BORDER);
		g.drawRect(barX, r.y + BAR_Y, barW - 1, BAR_HEIGHT - 1);
	}

	/** Experience in the vanilla skill tooltip's layout. */
	private String xpTooltip(InstrumentDef def)
	{
		int xp = skills.getXp(def.getType());
		String text = "Instrument XP: " + QuantityFormatter.formatNumber(xp);
		InstrumentRank next = InstrumentRank.of(xp).next();
		if (next == null)
		{
			return text;
		}
		return text + "</br>" + next.getLabel() + " at: " + QuantityFormatter.formatNumber(next.getMinXp())
			+ "</br>Remaining XP: " + QuantityFormatter.formatNumber(next.getMinXp() - xp);
	}

	private List<Row> storageRows()
	{
		List<Row> sorted = new ArrayList<>();
		for (InstrumentInstance instance : storage.all())
		{
			instruments.find(instance.getInstrumentId()).ifPresent(def -> sorted.add(new Row(new Rectangle(), def, instance)));
		}
		sorted.sort(Comparator.comparingInt(row -> row.getDef().getLevelRequired()));
		return sorted;
	}

	/** One row per instrument type, unlocked or not, named after the standard instrument. */
	private List<Row> levelRows()
	{
		List<Row> sorted = new ArrayList<>();
		for (InstrumentDef def : instruments.all())
		{
			if (!def.isLegendary())
			{
				sorted.add(new Row(new Rectangle(), def, null));
			}
		}
		sorted.sort(Comparator.comparingInt(row -> row.getDef().getLevelRequired()));
		return sorted;
	}

	/** Green when in tune, through yellow and orange, to red when extremely out of tune. */
	static Color tuningColor(int tuning)
	{
		switch (TuningState.of(tuning))
		{
			case IN_TUNE:
				return Color.GREEN;
			case SLIGHTLY_OUT:
				return Color.YELLOW;
			case OUT:
				return ORANGE;
			default:
				return new Color(0xFF3030);
		}
	}

	private static void drawText(Graphics2D g, String text, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		g.setColor(color);
		g.drawString(text, x, y);
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e)
	{
		if (view != View.MUSIC && listRect.contains(e.getPoint()))
		{
			scroll = Math.max(0, scroll + e.getWheelRotation() * view.rowHeight);
			e.consume();
		}
		return e;
	}
}
