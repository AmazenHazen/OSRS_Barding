package com.barding.ui;

import com.barding.data.CraftRecipe;
import com.barding.data.DuelDef;
import com.barding.data.DuelRepository;
import com.barding.data.GuideBlock;
import com.barding.data.GuideData;
import com.barding.data.GuidePage;
import com.barding.data.GuideTab;
import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.barding.data.Material;
import com.barding.data.QuestDef;
import com.barding.data.QuestRepository;
import com.barding.data.RiffText;
import com.barding.data.SkillRequirement;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import java.awt.image.BufferedImage;

/**
 * The Barding skill guide: our own window, drawn as an overlay in the middle of the game viewport.
 * It does not depend on any game interface, so it works the same in every client mode and
 * does not break when the game updates its widgets.
 *
 * Content comes from guide.json (pages, tabs, blocks), instruments.json, duels.json and quests.json.
 */
@Singleton
public class GuideWindow extends Overlay implements MouseListener, MouseWheelListener
{
	// Layout, in pixels. The size matches the game's own skill guide window.
	private static final int WIDTH = 512;
	private static final int HEIGHT = 334;
	private static final int TITLE_HEIGHT = 40;
	private static final int PAD = 8;
	private static final int LIST_WIDTH = 150;
	private static final int ROW_HEIGHT = 20;
	private static final int TAB_HEIGHT = 25;
	private static final int SCROLLBAR_WIDTH = 10;
	private static final int SCROLL_STEP = 24;

	// Colours. Tweak these until it looks right next to the real guide windows.
	private static final Color FRAME = new Color(0x4E4533);
	private static final Color EDGE_LIGHT = new Color(0x6E6350);
	private static final Color EDGE_DARK = new Color(0x2B261C);
	private static final Color PANEL = new Color(0x3A3326);
	private static final Color ROW_SELECTED = new Color(0x726451);
	private static final Color ROW_HOVER = new Color(0x5A503E);
	private static final Color CLOSE = new Color(0x7A2E22);
	private static final Color CLOSE_HOVER = new Color(0xA03A2A);
	private static final Color ORANGE = new Color(0xFF981F);
	private static final Color WHITE = Color.WHITE;
	private static final Color GREY = new Color(0xB0A890);

	private final Client client;
	private final InstrumentIcons icons;
	private GuideData guide;
	private InstrumentRepository instruments;
	private DuelRepository duels;
	private QuestRepository quests;

	// State shared between the client thread (render) and the AWT thread (mouse).
	private volatile boolean visible;
	private volatile int pageIndex;
	private volatile int tabIndex;
	private volatile int scroll;
	private volatile int maxScroll;
	private volatile Point mouse = new Point(-1, -1);

	// Hit boxes, rebuilt every frame by render().
	private volatile Rectangle windowRect = new Rectangle();
	private volatile Rectangle closeRect = new Rectangle();
	private volatile List<Rectangle> pageRects = new ArrayList<>();
	private volatile List<Rectangle> tabRects = new ArrayList<>();

	// Wrapped text is cached until the page, tab or width changes.
	private String cachedKey = "";
	private List<Line> cachedLines = new ArrayList<>();
	private int cachedHeight;

	@Inject
	public GuideWindow(Client client, InstrumentIcons icons)
	{
		this.client = client;
		this.icons = icons;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGHEST);
	}

	public void setData(GuideData guide, InstrumentRepository instruments, DuelRepository duels, QuestRepository quests)
	{
		this.guide = guide;
		this.instruments = instruments;
		this.duels = duels;
		this.quests = quests;
		this.cachedKey = "";
	}

	public void toggle()
	{
		if (visible)
		{
			close();
		}
		else
		{
			open();
		}
	}

	public void open()
	{
		pageIndex = 0;
		tabIndex = 0;
		scroll = 0;
		visible = true;
	}

	public void close()
	{
		visible = false;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!visible || guide == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		List<GuidePage> pages = guide.getPages();
		int pIdx = Math.max(0, Math.min(pageIndex, pages.size() - 1));
		GuidePage page = pages.get(pIdx);
		int tIdx = Math.max(0, Math.min(tabIndex, page.getTabs().size() - 1));
		GuideTab tab = page.getTabs().get(tIdx);
		boolean hasTabs = page.getTabs().size() > 1;
		Point m = mouse;

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		Font body = FontManager.getRunescapeFont();
		Font bold = FontManager.getRunescapeBoldFont();
		FontMetrics fm = g.getFontMetrics(body);

		// Layout: centred in the game viewport
		int x = client.getViewportXOffset() + Math.max(0, (client.getViewportWidth() - WIDTH) / 2);
		int y = client.getViewportYOffset() + Math.max(0, (client.getViewportHeight() - HEIGHT) / 2);
		Rectangle window = new Rectangle(x, y, WIDTH, HEIGHT);
		Rectangle close = new Rectangle(x + WIDTH - PAD - 26, y + PAD, 26, 23);
		Rectangle list = new Rectangle(x + PAD, y + TITLE_HEIGHT + PAD, LIST_WIDTH, HEIGHT - TITLE_HEIGHT - 2 * PAD);
		int rightX = list.x + list.width + PAD;
		int rightW = x + WIDTH - PAD - rightX;
		int tabsH = hasTabs ? TAB_HEIGHT + 4 : 0;
		Rectangle content = new Rectangle(rightX, list.y + tabsH, rightW, list.height - tabsH);

		// Window
		g.setColor(FRAME);
		g.fill(window);
		g.setColor(EDGE_LIGHT);
		g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
		g.setColor(EDGE_DARK);
		g.drawRect(x + 1, y + 1, WIDTH - 3, HEIGHT - 3);

		// Title
		g.setFont(bold);
		String title = "Barding - " + page.getTitle();
		int titleW = g.getFontMetrics().stringWidth(title);
		drawText(g, title, x + (WIDTH - titleW) / 2, y + 27, ORANGE);

		// Close button
		g.setColor(close.contains(m) ? CLOSE_HOVER : CLOSE);
		g.fill(close);
		g.setColor(EDGE_LIGHT);
		g.drawRect(close.x, close.y, close.width - 1, close.height - 1);
		g.setColor(WHITE);
		g.drawLine(close.x + 7, close.y + 6, close.x + 18, close.y + 16);
		g.drawLine(close.x + 18, close.y + 6, close.x + 7, close.y + 16);

		// Left-hand page list
		g.setColor(PANEL);
		g.fill(list);
		List<Rectangle> newPageRects = new ArrayList<>();
		g.setFont(body);
		for (int i = 0; i < pages.size(); i++)
		{
			Rectangle row = new Rectangle(list.x + 2, list.y + 2 + i * ROW_HEIGHT, list.width - 4, ROW_HEIGHT);
			newPageRects.add(row);
			if (i == pIdx)
			{
				g.setColor(ROW_SELECTED);
				g.fill(row);
			}
			else if (row.contains(m))
			{
				g.setColor(ROW_HOVER);
				g.fill(row);
			}
			drawText(g, pages.get(i).getTitle(), row.x + 8, row.y + 15, i == pIdx ? WHITE : ORANGE);
		}

		// Tab strip
		List<Rectangle> newTabRects = new ArrayList<>();
		if (hasTabs)
		{
			int count = page.getTabs().size();
			int tabW = rightW / count;
			for (int i = 0; i < count; i++)
			{
				Rectangle r = new Rectangle(rightX + i * tabW, list.y, tabW - 2, TAB_HEIGHT);
				newTabRects.add(r);
				g.setColor(i == tIdx ? ROW_SELECTED : (r.contains(m) ? ROW_HOVER : PANEL));
				g.fill(r);
				String label = page.getTabs().get(i).getTitle();
				int labelW = fm.stringWidth(label);
				drawText(g, label, r.x + (r.width - labelW) / 2, r.y + 17, i == tIdx ? WHITE : ORANGE);
			}
		}

		// Content
		g.setColor(PANEL);
		g.fill(content);
		int viewH = content.height - 8;
		int textW = content.width - 16 - SCROLLBAR_WIDTH;
		List<Line> lines = linesFor(pIdx, tIdx, tab, textW, fm);
		maxScroll = Math.max(0, cachedHeight - viewH);
		int s = Math.max(0, Math.min(maxScroll, scroll));
		scroll = s;

		Shape oldClip = g.getClip();
		g.setClip(content.x + 2, content.y + 2, content.width - 4, content.height - 4);
		int lineH = fm.getHeight();
		int cursor = 0;
		for (Line line : lines)
		{
			cursor += line.gap;
			int h = line.height(fm);
			int top = content.y + 4 + cursor - s;
			if (top + h > content.y && top < content.y + content.height)
			{
				int textX = content.x + 6 + line.indent;
				if (line.icon != null)
				{
					g.drawImage(line.icon, textX, top + (h - line.icon.getHeight()) / 2, null);
					textX += line.icon.getWidth() + 6;
				}
				g.setFont(line.bold ? bold : body);
				drawText(g, line.text, textX, top + (h - lineH) / 2 + lineH - 4, line.color);
			}
			cursor += h;
		}
		g.setClip(oldClip);

		// Scroll bar
		if (maxScroll > 0)
		{
			int trackX = content.x + content.width - SCROLLBAR_WIDTH - 3;
			int trackY = content.y + 2;
			int trackH = content.height - 4;
			g.setColor(EDGE_DARK);
			g.fillRect(trackX, trackY, SCROLLBAR_WIDTH, trackH);
			int thumbH = Math.max(20, trackH * viewH / cachedHeight);
			int thumbY = trackY + (trackH - thumbH) * s / maxScroll;
			g.setColor(ROW_SELECTED);
			g.fillRect(trackX + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbH);
		}

		// Publish hit boxes for the mouse handlers
		windowRect = window;
		closeRect = close;
		pageRects = newPageRects;
		tabRects = newTabRects;
		return null;
	}

	private static void drawText(Graphics2D g, String text, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		g.setColor(color);
		g.drawString(text, x, y);
	}

	// ---------------------------------------------------------------- content

	private static final class Line
	{
		final String text;
		final Color color;
		final int indent;
		final boolean bold;
		final int gap; // extra space above this line
		final BufferedImage icon; // optional, drawn to the left of the text

		Line(String text, Color color, int indent, boolean bold, int gap)
		{
			this(text, color, indent, bold, gap, null);
		}

		Line(String text, Color color, int indent, boolean bold, int gap, BufferedImage icon)
		{
			this.text = text;
			this.color = color;
			this.indent = indent;
			this.bold = bold;
			this.gap = gap;
			this.icon = icon;
		}

		/** Normal text height, or the icon's height if that is taller. */
		int height(FontMetrics fm)
		{
			return icon == null ? fm.getHeight() : Math.max(fm.getHeight(), icon.getHeight() + 2);
		}
	}

	private List<Line> linesFor(int page, int tab, GuideTab tabData, int width, FontMetrics fm)
	{
		String key = page + ":" + tab + ":" + width;
		if (!key.equals(cachedKey))
		{
			cachedLines = buildLines(tabData, width, fm);
			int height = 0;
			for (Line line : cachedLines)
			{
				height += line.gap + line.height(fm);
			}
			cachedHeight = height;
			cachedKey = key;
		}
		return cachedLines;
	}

	private List<Line> buildLines(GuideTab tab, int width, FontMetrics fm)
	{
		List<Line> out = new ArrayList<>();
		boolean first = true;
		for (GuideBlock block : tab.getBlocks())
		{
			int gap = first ? 0 : 8;
			first = false;
			switch (block.getType())
			{
				case "heading":
					out.add(new Line(block.getText(), WHITE, 0, true, gap));
					break;
				case "text":
					addWrapped(out, block.getText(), fm, width, ORANGE, 0, gap);
					break;
				case "bullets":
				{
					boolean firstItem = true;
					for (String item : block.getItems())
					{
						addWrapped(out, "- " + item, fm, width - 8, ORANGE, 8, firstItem ? gap : 3);
						firstItem = false;
					}
					break;
				}
				case "instruments":
					addInstruments(out, fm, width, gap, false);
					break;
				case "legendaries":
					addInstruments(out, fm, width, gap, true);
					break;
				case "duels":
					addDuels(out, fm, width, gap);
					break;
				case "quests":
					addQuests(out, fm, width, gap);
					break;
				default:
					break;
			}
		}
		return out;
	}

	private void addInstruments(List<Line> out, FontMetrics fm, int width, int gap, boolean legendary)
	{
		if (instruments == null)
		{
			out.add(new Line("No instrument data loaded.", ORANGE, 0, false, gap));
			return;
		}
		List<InstrumentDef> sorted = new ArrayList<>(instruments.all());
		sorted.sort(Comparator.comparingInt(InstrumentDef::getLevelRequired));
		int sub = InstrumentIcons.SIZE + 6; // the lines under a name line up with the name, not the icon
		boolean first = true;
		for (InstrumentDef def : sorted)
		{
			if (def.isLegendary() != legendary)
			{
				continue;
			}
			out.add(new Line("Lv " + def.getLevelRequired() + "  " + def.getName(), WHITE, 0, true,
					first ? gap : 8, icons.get(def.getId())));
			first = false;
			if (def.getRiff() != null)
			{
				addWrapped(out, RiffText.describe(def.getRiff()), fm, width - sub, ORANGE, sub, 0);
			}
			if (def.getCraft() != null)
			{
				addWrapped(out, "Craft: " + craftText(def.getCraft()), fm, width - sub, GREY, sub, 0);
			}
		}
	}

	private void addDuels(List<Line> out, FontMetrics fm, int width, int gap)
	{
		if (duels == null || instruments == null || quests == null)
		{
			out.add(new Line("No duel data loaded.", ORANGE, 0, false, gap));
			return;
		}
		List<DuelDef> sorted = new ArrayList<>(duels.all());
		sorted.sort(Comparator.comparingInt(DuelDef::getLevelRequired));
		int sub = InstrumentIcons.SIZE + 6;
		boolean first = true;
		for (DuelDef duel : sorted)
		{
			InstrumentDef legendary = instruments.get(duel.getLegendaryId());
			InstrumentDef needed = instruments.get(legendary.getBaseId());
			out.add(new Line("Lv " + duel.getLevelRequired() + "  " + duel.getName(), WHITE, 0, true,
					first ? gap : 8, icons.get(legendary.getId())));
			first = false;
			addWrapped(out, duel.getLocation() + " - " + needed.getName(), fm, width - sub, ORANGE, sub, 0);
			addWrapped(out, "Legendary: " + legendary.getName(), fm, width - sub, GREY, sub, 0);
			if (duel.getSong() != null)
			{
				addWrapped(out, "Song: " + duel.getSong(), fm, width - sub, GREY, sub, 0);
			}
			if (duel.getQuestId() != null)
			{
				addWrapped(out, "Requires: " + quests.get(duel.getQuestId()).getName(), fm, width - sub, GREY, sub, 0);
			}
		}
	}

	private void addQuests(List<Line> out, FontMetrics fm, int width, int gap)
	{
		if (quests == null)
		{
			out.add(new Line("No quest data loaded.", ORANGE, 0, false, gap));
			return;
		}
		boolean first = true;
		for (QuestDef quest : quests.all())
		{
			out.add(new Line(quest.getName(), WHITE, 0, true, first ? gap : 10));
			first = false;
			String summary = RiffText.capitalize(quest.getDifficulty().name()) + " quest";
			addWrapped(out, quest.getLocation() == null ? summary : summary + " - " + quest.getLocation(), fm, width, GREY, 0, 0);
			addWrapped(out, quest.getDescription(), fm, width, ORANGE, 0, 3);

			List<String> requirements = new ArrayList<>();
			for (SkillRequirement r : quest.getSkillRequirements())
			{
				requirements.add(r.getLevel() + " " + RiffText.capitalize(r.getSkill()));
			}
			requirements.addAll(quest.getQuestRequirements());
			if (!requirements.isEmpty())
			{
				addWrapped(out, "Requires: " + String.join(", ", requirements), fm, width, GREY, 0, 3);
			}

			addWrapped(out, "Rewards:", fm, width, WHITE, 0, 3);
			for (String reward : quest.getRewards())
			{
				addWrapped(out, "- " + reward, fm, width - 8, ORANGE, 8, 0);
			}
		}
	}

	private static String craftText(CraftRecipe craft)
	{
		StringBuilder materials = new StringBuilder();
		for (Material m : craft.getMaterials())
		{
			if (materials.length() > 0)
			{
				materials.append(", ");
			}
			materials.append(m.getQuantity()).append("x ").append(m.getItem());
		}
		List<String> requirements = new ArrayList<>();
		for (SkillRequirement r : craft.getRequirements())
		{
			requirements.add(r.getLevel() + " " + RiffText.capitalize(r.getSkill()));
		}
		return requirements.isEmpty() ? materials.toString() : materials + " (" + String.join(" & ", requirements) + ")";
	}

	private static void addWrapped(List<Line> out, String text, FontMetrics fm, int width, Color color, int indent, int gap)
	{
		if (text == null)
		{
			return;
		}
		boolean firstLine = true;
		for (String paragraph : text.split("\n"))
		{
			for (String part : wrap(paragraph, fm, width))
			{
				out.add(new Line(part, color, indent, false, firstLine ? gap : 0));
				firstLine = false;
			}
		}
	}

	private static List<String> wrap(String text, FontMetrics fm, int maxWidth)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.split(" "))
		{
			String candidate = current.length() == 0 ? word : current + " " + word;
			if (current.length() > 0 && fm.stringWidth(candidate) > maxWidth)
			{
				lines.add(current.toString());
				current = new StringBuilder(word);
			}
			else
			{
				current = new StringBuilder(candidate);
			}
		}
		if (current.length() > 0)
		{
			lines.add(current.toString());
		}
		return lines;
	}

	// ---------------------------------------------------------------- input

	private void handleClick(Point p)
	{
		if (closeRect.contains(p))
		{
			close();
			return;
		}
		List<Rectangle> pagesBoxes = pageRects;
		for (int i = 0; i < pagesBoxes.size(); i++)
		{
			if (pagesBoxes.get(i).contains(p))
			{
				pageIndex = i;
				tabIndex = 0;
				scroll = 0;
				return;
			}
		}
		List<Rectangle> tabBoxes = tabRects;
		for (int i = 0; i < tabBoxes.size(); i++)
		{
			if (tabBoxes.get(i).contains(p))
			{
				tabIndex = i;
				scroll = 0;
				return;
			}
		}
	}

	private MouseEvent consumeIfInside(MouseEvent e)
	{
		if (visible && windowRect.contains(e.getPoint()))
		{
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		if (visible && windowRect.contains(e.getPoint()))
		{
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				handleClick(e.getPoint());
			}
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		return consumeIfInside(e);
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		return consumeIfInside(e);
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		mouse = e.getPoint();
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		mouse = e.getPoint();
		return e;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e)
	{
		if (visible && windowRect.contains(e.getPoint()))
		{
			scroll = Math.max(0, Math.min(maxScroll, scroll + e.getWheelRotation() * SCROLL_STEP));
			e.consume();
		}
		return e;
	}
}
