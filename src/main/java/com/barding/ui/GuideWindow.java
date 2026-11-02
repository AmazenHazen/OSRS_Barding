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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ImageUtil;

/**
 * The Barding skill guide: our own window, drawn as an overlay in the middle of the game viewport.
 * It does not depend on any game interface, so it works the same in every client mode and
 * does not break when the game updates its widgets. It uses the game's own sprites, and its
 * layout and colours are measured from the vanilla skill guide.
 *
 * Content comes from guide.json (pages, tabs, blocks), instruments.json, duels.json and quests.json.
 */
@Singleton
public class GuideWindow extends Overlay implements MouseListener, MouseWheelListener
{
	// Layout, in pixels from the window's top left. Measured from the vanilla skill guide.
	private static final int WIDTH = 512;
	private static final int HEIGHT = 334;
	private static final int TITLE_BASELINE = 25;
	private static final int TITLE_ICON_HEIGHT = 16;
	private static final int DIVIDER_Y = 33;
	private static final int DIVIDER_HEIGHT = 6;
	private static final int CLOSE_RIGHT = 10;
	private static final int CLOSE_Y = 8;
	private static final Rectangle BOX = new Rectangle(156, 43, 346, 281);
	private static final int LIST_X = 10;
	private static final int LIST_WIDTH = 147;
	private static final int ROW_HEIGHT = 18;
	private static final int ROW_PITCH = 20;
	private static final int CONTENT_X = 167;
	private static final int CONTENT_TOP = 53;
	private static final int CONTENT_TOP_TABS = 71;
	private static final int CONTENT_BOTTOM = 314;
	private static final int CONTENT_WIDTH = 310;
	private static final int TABS_Y = 49;
	private static final int SCROLLBAR_WIDTH = 16;
	private static final int SCROLL_STEP = 24;

	// A list entry: level, then icon, then text. Short entries are padded to the vanilla stripe height.
	private static final int ENTRY_MIN_HEIGHT = 36;
	private static final int ENTRY_PAD = 4;
	private static final int ENTRY_ICON_X = 26;
	private static final int ENTRY_TEXT_X = 62;

	// Colours, sampled from the vanilla skill guide
	private static final Color FRAME = new Color(0x483E33);
	private static final Color BORDER = new Color(0x726451);
	private static final Color SHADOW = new Color(0x2E2B23);
	private static final Color ROW = new Color(0x3F362B);
	private static final Color ROW_HOVER = new Color(0x4F4537);
	private static final Color EDGE_DARK = new Color(0x0E0E0C);
	private static final Color EDGE_LIGHT = new Color(0x474745);
	private static final Color DIVIDER_DARK = new Color(0x1C1C1A);
	private static final Color DIVIDER_LIGHT = new Color(0x4E4D4B);
	private static final Color STRIPE_DARK = new Color(0x393228);
	private static final Color STRIPE_LIGHT = new Color(0x3D352B);
	private static final Color CLOSE = new Color(0x7A2E22);
	private static final Color ORANGE = new Color(0xFF981F);
	private static final Color WHITE = Color.WHITE;
	private static final Color GREY = new Color(0xB0A890);

	private final Client client;
	private final SpriteManager sprites;
	private final InstrumentIcons icons;
	private GuideData guide;
	private InstrumentRepository instruments;
	private DuelRepository duels;
	private QuestRepository quests;

	// Sprites, fetched once on the first render
	private BufferedImage chrome;
	private BufferedImage titleIcon;
	private BufferedImage closeUp;
	private BufferedImage closeHover;
	private BufferedImage arrowUp;
	private BufferedImage arrowDown;
	private BufferedImage track;
	private BufferedImage thumbTop;
	private BufferedImage thumbMiddle;
	private BufferedImage thumbBottom;

	// State shared between the client thread (render) and the AWT thread (mouse).
	private volatile boolean visible;
	private volatile int pageIndex;
	private volatile int tabIndex;
	private volatile int scroll;
	private volatile int maxScroll;
	private volatile boolean dragging;
	private volatile int dragOffset;
	private volatile Point mouse = new Point(-1, -1);

	// Hit boxes, rebuilt every frame by render().
	private volatile Rectangle windowRect = new Rectangle();
	private volatile Rectangle closeRect = new Rectangle();
	private volatile Rectangle upRect = new Rectangle();
	private volatile Rectangle downRect = new Rectangle();
	private volatile Rectangle trackRect = new Rectangle();
	private volatile Rectangle thumbRect = new Rectangle();
	private volatile List<Rectangle> pageRects = new ArrayList<>();
	private volatile List<Rectangle> tabRects = new ArrayList<>();

	// Wrapped text is cached until the page, tab or width changes.
	private String cachedKey = "";
	private List<Line> cachedLines = new ArrayList<>();
	private int cachedHeight;

	@Inject
	public GuideWindow(Client client, SpriteManager sprites, InstrumentIcons icons)
	{
		this.client = client;
		this.sprites = sprites;
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

	public boolean isVisible()
	{
		return visible;
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
		dragging = false;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!visible || guide == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		if (chrome == null)
		{
			loadSprites();
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

		g.drawImage(chrome, x, y, null);

		// Title, with the icon just left of the text
		g.setFont(bold);
		String title = "Barding - " + page.getTitle();
		int titleW = g.getFontMetrics().stringWidth(title);
		int iconW = titleIcon.getWidth() + 5;
		int titleX = x + (WIDTH - titleW - iconW) / 2;
		g.drawImage(titleIcon, titleX, y + 4 + (DIVIDER_Y - 4 - titleIcon.getHeight()) / 2, null);
		drawText(g, title, titleX + iconW, y + TITLE_BASELINE, ORANGE);

		// Close button
		int closeW = closeUp == null ? 26 : closeUp.getWidth();
		int closeH = closeUp == null ? 23 : closeUp.getHeight();
		Rectangle close = new Rectangle(x + WIDTH - CLOSE_RIGHT - closeW, y + CLOSE_Y, closeW, closeH);
		if (closeUp == null)
		{
			g.setColor(CLOSE);
			g.fill(close);
		}
		else
		{
			g.drawImage(close.contains(m) && closeHover != null ? closeHover : closeUp, close.x, close.y, null);
		}

		// Left-hand page list: the selected row is open on its right, joining it to the content box
		g.setFont(body);
		List<Rectangle> newPageRects = new ArrayList<>();
		for (int i = 0; i < pages.size(); i++)
		{
			Rectangle row = new Rectangle(x + LIST_X, y + BOX.y + i * ROW_PITCH, LIST_WIDTH, ROW_HEIGHT);
			newPageRects.add(row);
			g.setColor(SHADOW);
			g.fillRect(row.x - 1, row.y, 1, ROW_PITCH);
			g.fillRect(row.x, row.y + ROW_HEIGHT, row.width - 1, ROW_PITCH - ROW_HEIGHT);
			drawRow(g, row, i == pIdx, row.contains(m));
			if (i == pIdx)
			{
				g.setColor(FRAME);
				g.fillRect(row.x + row.width - 1, row.y + 1, 1, ROW_HEIGHT - 2);
			}
			drawText(g, pages.get(i).getTitle(), row.x + 7, row.y + 14, ORANGE);
		}

		// Tab strip
		int contentX = x + CONTENT_X;
		int stripW = CONTENT_WIDTH + SCROLLBAR_WIDTH;
		List<Rectangle> newTabRects = new ArrayList<>();
		if (hasTabs)
		{
			int count = page.getTabs().size();
			int tabW = (stripW + 2) / count;
			for (int i = 0; i < count; i++)
			{
				Rectangle r = new Rectangle(contentX + i * tabW, y + TABS_Y, tabW - 2, ROW_HEIGHT);
				newTabRects.add(r);
				drawRow(g, r, i == tIdx, r.contains(m));
				String label = page.getTabs().get(i).getTitle();
				drawText(g, label, r.x + (r.width - fm.stringWidth(label)) / 2, r.y + 14, ORANGE);
			}
		}

		// Content frame
		int contentY = y + (hasTabs ? CONTENT_TOP_TABS : CONTENT_TOP);
		Rectangle frame = new Rectangle(contentX, contentY, CONTENT_WIDTH, y + CONTENT_BOTTOM - contentY);
		g.setColor(STRIPE_DARK);
		g.fill(frame);
		g.setColor(EDGE_DARK);
		g.drawRect(frame.x, frame.y, frame.width - 1, frame.height - 1);
		g.setColor(EDGE_LIGHT);
		g.drawRect(frame.x + 1, frame.y + 1, frame.width - 3, frame.height - 3);

		// Content
		Rectangle view = new Rectangle(frame.x + 2, frame.y + 2, frame.width - 4, frame.height - 4);
		List<Line> lines = linesFor(pIdx, tIdx, tab, view.width - 12, fm);
		maxScroll = Math.max(0, cachedHeight - view.height);
		int s = Math.max(0, Math.min(maxScroll, scroll));
		scroll = s;

		Shape oldClip = g.getClip();
		g.setClip(view);
		int lineH = fm.getHeight();
		for (Line line : lines)
		{
			if (line.stripe != null)
			{
				g.setColor(line.stripe);
				g.fillRect(view.x, view.y + line.y - s - line.gap, view.width, line.gap + lineH + line.after);
			}
		}
		for (Line line : lines)
		{
			int top = view.y + line.y - s;
			int height = Math.max(lineH, line.entryHeight);
			if (top + height <= view.y || top - line.gap >= view.y + view.height)
			{
				continue;
			}
			int textX = view.x + 6 + line.indent;
			int centre = top - line.gap + line.entryHeight / 2;
			g.setFont(body);
			if (line.level != null)
			{
				drawText(g, line.level, view.x + 6, centre + 4, ORANGE);
			}
			if (line.icon != null)
			{
				int iconX = view.x + 6 + ENTRY_ICON_X + (InstrumentIcons.SIZE - line.icon.getWidth()) / 2;
				g.drawImage(line.icon, iconX, centre - line.icon.getHeight() / 2, null);
			}
			g.setFont(line.bold ? bold : body);
			drawText(g, line.text, textX, top + lineH - 4, line.color);
		}
		g.setClip(oldClip);

		drawScrollbar(g, frame, view.height);

		// Publish hit boxes for the mouse handlers
		windowRect = window;
		closeRect = close;
		pageRects = newPageRects;
		tabRects = newTabRects;
		return null;
	}

	/** Fetches the game's sprites and draws everything that never changes into one image. */
	private void loadSprites()
	{
		BufferedImage backing = sprites.getSprite(SpriteID.TRADEBACKING, 0);
		BufferedImage edgeTop = sprites.getSprite(SpriteID.Steelborder2.EDGE_TOP, 0);
		BufferedImage edgeRight = sprites.getSprite(SpriteID.Steelborder2.EDGE_RIGHT, 0);
		BufferedImage topLeft = sprites.getSprite(SpriteID.Steelborder.TOP_LEFT, 0);
		BufferedImage topRight = sprites.getSprite(SpriteID.Steelborder.TOP_RIGHT, 0);
		BufferedImage bottomLeft = sprites.getSprite(SpriteID.Steelborder.BOTTOM_LEFT, 0);
		BufferedImage bottomRight = sprites.getSprite(SpriteID.Steelborder.BOTTOM_RIGHT, 0);
		closeUp = sprites.getSprite(SpriteID.SteelborderCloseButton._0, 0);
		closeHover = sprites.getSprite(SpriteID.SteelborderCloseButton._1, 0);
		arrowUp = sprites.getSprite(SpriteID.ScrollbarV2.ARROW_UP, 0);
		arrowDown = sprites.getSprite(SpriteID.ScrollbarV2.ARROW_DOWN, 0);
		track = sprites.getSprite(SpriteID.ScrollbarDraggerV2.TRACK, 0);
		thumbTop = sprites.getSprite(SpriteID.ScrollbarDraggerV2.TOP, 0);
		thumbMiddle = sprites.getSprite(SpriteID.ScrollbarDraggerV2.MIDDLE, 0);
		thumbBottom = sprites.getSprite(SpriteID.ScrollbarDraggerV2.BOTTOM, 0);
		titleIcon = icons.skillIcon(TITLE_ICON_HEIGHT);

		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(FRAME);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		tile(g, backing, new Rectangle(0, 0, WIDTH, HEIGHT));

		// Title divider
		g.setColor(DIVIDER_DARK);
		g.fillRect(0, DIVIDER_Y, WIDTH, DIVIDER_HEIGHT);
		g.setColor(DIVIDER_LIGHT);
		g.fillRect(0, DIVIDER_Y, WIDTH, 1);
		g.fillRect(0, DIVIDER_Y + DIVIDER_HEIGHT - 1, WIDTH, 1);

		// The box around the content
		g.setColor(SHADOW);
		g.drawRect(BOX.x, BOX.y - 1, BOX.width, BOX.height + 1);
		g.setColor(BORDER);
		g.drawRect(BOX.x, BOX.y, BOX.width - 1, BOX.height - 1);

		// Steel border: the game only has top and right edges, so the others are mirrored
		if (edgeTop != null && edgeRight != null)
		{
			tile(g, edgeTop, new Rectangle(0, 0, WIDTH, edgeTop.getHeight()));
			tile(g, ImageUtil.flipImage(edgeTop, false, true),
				new Rectangle(0, HEIGHT - edgeTop.getHeight(), WIDTH, edgeTop.getHeight()));
			tile(g, ImageUtil.flipImage(edgeRight, true, false),
				new Rectangle(0, 0, edgeRight.getWidth(), HEIGHT));
			tile(g, edgeRight, new Rectangle(WIDTH - edgeRight.getWidth(), 0, edgeRight.getWidth(), HEIGHT));
		}
		if (topLeft != null && topRight != null && bottomLeft != null && bottomRight != null)
		{
			g.drawImage(topLeft, 0, 0, null);
			g.drawImage(topRight, WIDTH - topRight.getWidth(), 0, null);
			g.drawImage(bottomLeft, 0, HEIGHT - bottomLeft.getHeight(), null);
			g.drawImage(bottomRight, WIDTH - bottomRight.getWidth(), HEIGHT - bottomRight.getHeight(), null);
		}
		g.dispose();
		chrome = image;
	}

	private static void tile(Graphics2D g, BufferedImage image, Rectangle area)
	{
		if (image == null)
		{
			return;
		}
		Shape oldClip = g.getClip();
		g.clip(area);
		for (int y = area.y; y < area.y + area.height; y += image.getHeight())
		{
			for (int x = area.x; x < area.x + area.width; x += image.getWidth())
			{
				g.drawImage(image, x, y, null);
			}
		}
		g.setClip(oldClip);
	}

	private void drawScrollbar(Graphics2D g, Rectangle frame, int viewHeight)
	{
		int x = frame.x + frame.width;
		int arrowH = SCROLLBAR_WIDTH;
		Rectangle up = new Rectangle(x, frame.y, SCROLLBAR_WIDTH, arrowH);
		Rectangle down = new Rectangle(x, frame.y + frame.height - arrowH, SCROLLBAR_WIDTH, arrowH);
		Rectangle bar = new Rectangle(x, frame.y + arrowH, SCROLLBAR_WIDTH, frame.height - 2 * arrowH);

		int max = maxScroll;
		int thumbH = max > 0 ? Math.max(20, bar.height * viewHeight / cachedHeight) : bar.height;
		int thumbY = bar.y + (max > 0 ? (bar.height - thumbH) * scroll / max : 0);
		Rectangle thumb = new Rectangle(x, thumbY, SCROLLBAR_WIDTH, thumbH);

		g.setColor(EDGE_DARK);
		g.fillRect(x, frame.y, SCROLLBAR_WIDTH, frame.height);
		tile(g, track, bar);
		g.setColor(ROW_HOVER);
		g.fill(thumb);
		tile(g, thumbMiddle, thumb);
		if (thumbTop != null && thumbBottom != null)
		{
			g.drawImage(thumbTop, thumb.x, thumb.y, null);
			g.drawImage(thumbBottom, thumb.x, thumb.y + thumb.height - thumbBottom.getHeight(), null);
		}
		if (arrowUp != null && arrowDown != null)
		{
			g.drawImage(arrowUp, up.x, up.y, null);
			g.drawImage(arrowDown, down.x, down.y, null);
		}

		upRect = up;
		downRect = down;
		trackRect = bar;
		thumbRect = thumb;
	}

	/** A list row or tab. The selected one has the window's own background, like the vanilla guide. */
	private static void drawRow(Graphics2D g, Rectangle r, boolean selected, boolean hover)
	{
		g.setColor(selected ? FRAME : hover ? ROW_HOVER : ROW);
		g.fill(r);
		g.setColor(BORDER);
		g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
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
		int gap; // extra space above this line
		int after; // extra space below this line

		// Only set on the first line of a list entry, except stripe which is on all of them
		String level;
		BufferedImage icon;
		int entryHeight;
		Color stripe;

		int y; // top of the text, in pixels from the top of the content

		Line(String text, Color color, int indent, boolean bold, int gap)
		{
			this.text = text;
			this.color = color;
			this.indent = indent;
			this.bold = bold;
			this.gap = gap;
		}
	}

	private List<Line> linesFor(int page, int tab, GuideTab tabData, int width, FontMetrics fm)
	{
		String key = page + ":" + tab + ":" + width;
		if (!key.equals(cachedKey))
		{
			cachedLines = buildLines(tabData, width, fm);
			int cursor = 0;
			for (Line line : cachedLines)
			{
				cursor += line.gap;
				line.y = cursor;
				cursor += fm.getHeight() + line.after;
			}
			cachedHeight = cursor;
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
			int gap = first ? 4 : 8;
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
					addInstruments(out, fm, width, false);
					break;
				case "legendaries":
					addInstruments(out, fm, width, true);
					break;
				case "duels":
					addDuels(out, fm, width);
					break;
				case "quests":
					addQuests(out, fm, width);
					break;
				default:
					break;
			}
		}
		return out;
	}

	private void addInstruments(List<Line> out, FontMetrics fm, int width, boolean legendary)
	{
		if (instruments == null)
		{
			out.add(new Line("No instrument data loaded.", ORANGE, 0, false, 4));
			return;
		}
		List<InstrumentDef> sorted = new ArrayList<>(instruments.all());
		sorted.sort(Comparator.comparingInt(InstrumentDef::getLevelRequired));
		int textW = width - ENTRY_TEXT_X;
		int index = 0;
		for (InstrumentDef def : sorted)
		{
			if (def.isLegendary() != legendary)
			{
				continue;
			}
			int start = out.size();
			addEntryTitle(out, def.getName(), def.getLevelRequired(), def);
			if (def.getRiff() != null)
			{
				addWrapped(out, RiffText.describe(def.getRiff()), fm, textW, WHITE, ENTRY_TEXT_X, 0);
			}
			if (def.getCraft() != null)
			{
				addWrapped(out, "Craft: " + craftText(def.getCraft()), fm, textW, GREY, ENTRY_TEXT_X, 0);
			}
			finishEntry(out, start, fm, index++);
		}
	}

	private void addDuels(List<Line> out, FontMetrics fm, int width)
	{
		if (duels == null || instruments == null || quests == null)
		{
			out.add(new Line("No duel data loaded.", ORANGE, 0, false, 4));
			return;
		}
		List<DuelDef> sorted = new ArrayList<>(duels.all());
		sorted.sort(Comparator.comparingInt(DuelDef::getLevelRequired));
		int textW = width - ENTRY_TEXT_X;
		int index = 0;
		for (DuelDef duel : sorted)
		{
			InstrumentDef legendary = instruments.get(duel.getLegendaryId());
			InstrumentDef needed = instruments.get(legendary.getBaseId());
			int start = out.size();
			addEntryTitle(out, duel.getName(), duel.getLevelRequired(), legendary);
			addWrapped(out, duel.getLocation() + " - " + needed.getName(), fm, textW, WHITE, ENTRY_TEXT_X, 0);
			addWrapped(out, "Legendary: " + legendary.getName(), fm, textW, GREY, ENTRY_TEXT_X, 0);
			if (duel.getSong() != null)
			{
				addWrapped(out, "Song: " + duel.getSong(), fm, textW, GREY, ENTRY_TEXT_X, 0);
			}
			if (duel.getQuestId() != null)
			{
				addWrapped(out, "Requires: " + quests.get(duel.getQuestId()).getName(), fm, textW, GREY, ENTRY_TEXT_X, 0);
			}
			finishEntry(out, start, fm, index++);
		}
	}

	private void addQuests(List<Line> out, FontMetrics fm, int width)
	{
		if (quests == null)
		{
			out.add(new Line("No quest data loaded.", ORANGE, 0, false, 4));
			return;
		}
		int index = 0;
		for (QuestDef quest : quests.all())
		{
			int start = out.size();
			out.add(new Line(quest.getName(), ORANGE, 0, true, 0));
			String summary = RiffText.capitalize(quest.getDifficulty().name()) + " quest";
			addWrapped(out, quest.getLocation() == null ? summary : summary + " - " + quest.getLocation(), fm, width, GREY, 0, 0);
			addWrapped(out, quest.getDescription(), fm, width, WHITE, 0, 3);

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

			addWrapped(out, "Rewards:", fm, width, ORANGE, 0, 3);
			for (String reward : quest.getRewards())
			{
				addWrapped(out, "- " + reward, fm, width - 8, ORANGE, 8, 0);
			}
			finishEntry(out, start, fm, index++);
		}
	}

	private void addEntryTitle(List<Line> out, String name, int level, InstrumentDef iconOf)
	{
		Line title = new Line(name, ORANGE, ENTRY_TEXT_X, false, 0);
		title.level = Integer.toString(level);
		title.icon = icons.get(iconOf);
		out.add(title);
	}

	/** Pads the lines from start to the end of out into one striped entry, centred in its stripe. */
	private static void finishEntry(List<Line> out, int start, FontMetrics fm, int index)
	{
		int textHeight = 0;
		Color stripe = index % 2 == 0 ? STRIPE_DARK : STRIPE_LIGHT;
		for (Line line : out.subList(start, out.size()))
		{
			textHeight += line.gap + fm.getHeight();
			line.stripe = stripe;
		}
		int pad = Math.max(ENTRY_PAD, (ENTRY_MIN_HEIGHT - textHeight) / 2);
		Line first = out.get(start);
		first.gap = pad;
		first.entryHeight = textHeight + 2 * pad;
		out.get(out.size() - 1).after = pad;
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
		if (upRect.contains(p))
		{
			scrollBy(-SCROLL_STEP);
			return;
		}
		if (downRect.contains(p))
		{
			scrollBy(SCROLL_STEP);
			return;
		}
		if (thumbRect.contains(p))
		{
			dragging = true;
			dragOffset = p.y - thumbRect.y;
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

	private void scrollBy(int amount)
	{
		scroll = Math.max(0, Math.min(maxScroll, scroll + amount));
	}

	private void dragThumb(Point p)
	{
		int range = trackRect.height - thumbRect.height;
		if (range > 0)
		{
			scroll = Math.max(0, Math.min(maxScroll, (p.y - dragOffset - trackRect.y) * maxScroll / range));
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
		if (dragging)
		{
			dragging = false;
			e.consume();
			return e;
		}
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
		if (dragging)
		{
			dragThumb(e.getPoint());
			e.consume();
		}
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
			scrollBy(e.getWheelRotation() * SCROLL_STEP);
			e.consume();
		}
		return e;
	}
}
