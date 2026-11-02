package com.barding.ui;

import com.barding.progress.ProgressService;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

/** Adds a Barding tile to the in-game Skills tab (group 320) as a ninth row. */
@Singleton
public class SkillsTabTile
{
    // Skills tab geometry, measured with the Widget Inspector
    private static final int TILE_COUNT = 24; // Attack ... Sailing, one column after another
    private static final int ROWS = 8;
    private static final int[] COL_X = {1, 64, 127};
    private static final int TILE_W = 62;

    // Vanilla layout, used to undo our changes
    private static final int VANILLA_PITCH = 30;
    private static final int VANILLA_LAST_ROW_HEIGHT = 32;
    private static final int VANILLA_TOTAL_X = 0;
    private static final int VANILLA_TOTAL_Y = 241;
    private static final int VANILLA_TOTAL_WIDTH = 190;

    // Our layout: tweak these if it looks off
    private static final int ROW_PITCH = 29;
    private static final int TOP = 1;

    // Sprites copied from the vanilla Construction tile
    private static final int SPRITE_BG_LEFT = SpriteID.Miscgraphics2._2;
    private static final int SPRITE_BG_RIGHT = SpriteID.Miscgraphics2._3;
    private static final int BG_LEFT_WIDTH = 36;
    private static final int BG_RIGHT_X = 31;

    // Our own sprites. Negative ids can't clash with the game's.
    private static final int SPRITE_TILE = -25100;
    private static final int SPRITE_ICON = -25101;
    private static final int ICON_HEIGHT = 25;

    private static final int COLOR_YELLOW = 0xFFFF00;

    private final Client client;
    private final SpriteManager spriteManager;
    private final InstrumentIcons icons;
    private final ProgressService progress;
    private final List<Widget> ours = new ArrayList<>();
    private final List<Widget> levelTexts = new ArrayList<>();
    private Runnable onClick = () -> { };
    private Widget hitArea;
    private boolean hovered;
    private int shownLevel;

    @Inject
    public SkillsTabTile(Client client, SpriteManager spriteManager, InstrumentIcons icons, ProgressService progress)
    {
        this.client = client;
        this.spriteManager = spriteManager;
        this.icons = icons;
        this.progress = progress;
    }

    public void setOnClick(Runnable onClick)
    {
        this.onClick = onClick;
    }

    /** Call every client tick. Cheap: does nothing unless the tab needs to be (re)built. */
    public void update()
    {
        Widget root = client.getWidget(InterfaceID.Stats.UNIVERSE);
        Widget probe = client.getWidget(InterfaceID.Stats.CONSTRUCTION);
        if (root == null || probe == null)
        {
            return;
        }
        if (probe.getOriginalY() != rowY(ROWS - 1))
        {
            apply(root);
        }
        else if (shownLevel != progress.getLevel())
        {
            showLevel();
        }
    }

    /** True while the mouse is over the Barding tile. */
    public boolean isHovered()
    {
        return hovered && hitArea != null && !hitArea.isHidden();
    }

    private void showLevel()
    {
        shownLevel = progress.getLevel();
        for (Widget text : levelTexts)
        {
            text.setText(Integer.toString(shownLevel));
        }
    }

    private void apply(Widget root)
    {
        int rowY = rowY(ROWS);                 // the new ninth row
        int rowH = root.getHeight() - rowY;
        int x0 = COL_X[0];

        BufferedImage tileImage = buildTileImage(rowH);
        if (tileImage == null)
        {
            return; // sprites not ready yet: the next tick tries again
        }
        client.getSpriteOverrides().put(SPRITE_TILE, ImageUtil.getImageSpritePixels(tileImage, client));
        client.getSpriteOverrides().put(SPRITE_ICON, ImageUtil.getImageSpritePixels(icons.skillIcon(ICON_HEIGHT), client));

        ours.clear();
        levelTexts.clear();
        hovered = false;

        // 1. Squeeze the 24 vanilla tiles. The last row keeps its full height where it has room below
        // (Hunter and Sailing), because that tile's bottom border is in those last few pixels.
        for (int i = 0; i < TILE_COUNT; i++)
        {
            Widget tile = client.getWidget(InterfaceID.Stats.ATTACK + i);
            if (tile == null)
            {
                continue;
            }
            boolean aboveTotal = i >= ROWS && i % ROWS == ROWS - 1;
            tile.setOriginalY(rowY(i % ROWS));
            tile.setOriginalHeight(aboveTotal ? VANILLA_LAST_ROW_HEIGHT : ROW_PITCH);
            tile.revalidate();
        }

        // 2. Shrink the Total level strip into the two right-hand slots
        Widget total = client.getWidget(InterfaceID.Stats.TOTAL);
        if (total != null)
        {
            total.setOriginalX(COL_X[1]);
            total.setOriginalWidth(root.getWidth() - COL_X[1]);
            total.setOriginalY(rowY + (rowH - total.getOriginalHeight()) / 2);
            total.revalidate();
        }

        // 3. Build the Barding tile (flat children of the root)
        int iconW = 26 * ICON_HEIGHT / 31;
        addSprite(root, x0, rowY, TILE_W, rowH, SPRITE_TILE);
        addSprite(root, x0 + 3 + (ICON_HEIGHT - iconW) / 2, rowY + 2, iconW, ICON_HEIGHT, SPRITE_ICON);
        addText(root, x0 + 32, rowY + 4, 15, 12);
        addText(root, x0 + 44, rowY + 16, 15, 12);
        showLevel();
        addHitArea(root, x0, rowY, TILE_W, rowH);
    }

    /**
     * Joins Construction's two background sprites into one tile, then cuts rows out of the middle to
     * fit the shorter ninth row. Cropping would lose the bottom border; the middle is plain stone.
     */
    private BufferedImage buildTileImage(int height)
    {
        BufferedImage left = spriteManager.getSprite(SPRITE_BG_LEFT, 0);
        BufferedImage right = spriteManager.getSprite(SPRITE_BG_RIGHT, 0);
        if (left == null || right == null)
        {
            return null;
        }

        int fullHeight = Math.max(left.getHeight(), right.getHeight());
        BufferedImage full = new BufferedImage(TILE_W, fullHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = full.createGraphics();
        g.setClip(0, 0, BG_LEFT_WIDTH, fullHeight);
        g.drawImage(left, 0, 0, null);
        g.setClip(0, 0, TILE_W, fullHeight);
        g.drawImage(right, BG_RIGHT_X, 0, null);
        g.dispose();
        if (fullHeight <= height)
        {
            return full;
        }

        int top = height / 2;
        int bottom = height - top;
        BufferedImage squeezed = new BufferedImage(TILE_W, height, BufferedImage.TYPE_INT_ARGB);
        g = squeezed.createGraphics();
        g.drawImage(full.getSubimage(0, 0, TILE_W, top), 0, 0, null);
        g.drawImage(full.getSubimage(0, fullHeight - bottom, TILE_W, bottom), 0, top, null);
        g.dispose();
        return squeezed;
    }

    /** Undo everything. Call on the client thread when the plugin shuts down. */
    public void reset()
    {
        for (Widget w : ours)
        {
            w.setHidden(true);
        }
        ours.clear();
        levelTexts.clear();
        hitArea = null;
        hovered = false;
        client.getSpriteOverrides().remove(SPRITE_TILE);
        client.getSpriteOverrides().remove(SPRITE_ICON);

        for (int i = 0; i < TILE_COUNT; i++)
        {
            Widget tile = client.getWidget(InterfaceID.Stats.ATTACK + i);
            if (tile == null)
            {
                continue;
            }
            int row = i % ROWS;
            tile.setOriginalY(TOP + VANILLA_PITCH * row);
            tile.setOriginalHeight(row == ROWS - 1 ? VANILLA_LAST_ROW_HEIGHT : VANILLA_PITCH);
            tile.revalidate();
        }

        Widget total = client.getWidget(InterfaceID.Stats.TOTAL);
        if (total != null)
        {
            total.setOriginalX(VANILLA_TOTAL_X);
            total.setOriginalY(VANILLA_TOTAL_Y);
            total.setOriginalWidth(VANILLA_TOTAL_WIDTH);
            total.revalidate();
        }
    }

    private static int rowY(int row)
    {
        return TOP + ROW_PITCH * row;
    }

    private void addSprite(Widget parent, int x, int y, int w, int h, int spriteId)
    {
        Widget sprite = parent.createChild(-1, WidgetType.GRAPHIC);
        sprite.setSpriteId(spriteId);
        position(sprite, x, y, w, h);
        ours.add(sprite);
    }

    private void addText(Widget parent, int x, int y, int w, int h)
    {
        Widget text = parent.createChild(-1, WidgetType.TEXT);
        text.setFontId(FontID.PLAIN_11);
        text.setTextColor(COLOR_YELLOW);
        text.setTextShadowed(true);
        text.setXTextAlignment(1); // centered
        text.setYTextAlignment(1);
        position(text, x, y, w, h);
        ours.add(text);
        levelTexts.add(text);
    }

    private void addHitArea(Widget parent, int x, int y, int w, int h)
    {
        Widget hit = parent.createChild(-1, WidgetType.RECTANGLE);
        hit.setFilled(true);
        hit.setOpacity(255); // fully transparent
        hit.setHasListener(true);
        hit.setName("");
        hit.setAction(0, "View <col=ff981f>Barding</col> guide");
        hit.setOnOpListener((JavaScriptCallback) ev -> onClick.run());
        hit.setOnMouseOverListener((JavaScriptCallback) ev -> hovered = true);
        hit.setOnMouseLeaveListener((JavaScriptCallback) ev -> hovered = false);
        position(hit, x, y, w, h);
        ours.add(hit);
        hitArea = hit;
    }

    private static void position(Widget w, int x, int y, int width, int height)
    {
        w.setOriginalX(x);
        w.setOriginalY(y);
        w.setOriginalWidth(width);
        w.setOriginalHeight(height);
        w.revalidate();
    }
}