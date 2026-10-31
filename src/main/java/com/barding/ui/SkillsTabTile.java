package com.barding.ui;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/** Adds a Barding tile to the in-game Skills tab (group 320) as a ninth row. */
@Singleton
public class SkillsTabTile
{
    // Skills tab geometry, measured with the Widget Inspector
    private static final int GROUP = 320;
    private static final int UNIVERSE = 0;   // 320.0, the tab root
    private static final int TOTAL = 25;     // 320.25, the Total level strip
    private static final int FIRST_TILE = 1; // 320.1 Attack ... 320.24 Sailing
    private static final int TILE_COUNT = 24;
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
    private static final int SPRITE_BG_LEFT = 187;
    private static final int SPRITE_BG_RIGHT = 188;
    private static final int SPRITE_PLACEHOLDER_ICON = 7452; // Construction's icon, for now
    private static final int FONT_STAT = 494;
    private static final int COLOR_YELLOW = 0xFFFF00;

    private final Client client;
    private final List<Widget> ours = new ArrayList<>();
    private Runnable onClick = () -> { };
    private int level = 1; // later: comes from the XP service

    @Inject
    public SkillsTabTile(Client client)
    {
        this.client = client;
    }

    public void setOnClick(Runnable onClick)
    {
        this.onClick = onClick;
    }

    /** Call every client tick. Cheap: does nothing unless the tab needs to be (re)built. */
    public void update()
    {
        Widget root = client.getWidget(GROUP, UNIVERSE);
        Widget probe = client.getWidget(GROUP, FIRST_TILE + ROWS - 1); // Construction
        if (root == null || probe == null)
        {
            return;
        }
        if (probe.getOriginalY() == rowY(ROWS - 1))
        {
            return; // already applied
        }
        apply(root);
    }

    private void apply(Widget root)
    {
        ours.clear();

        // 1. Squeeze the 24 vanilla tiles
        for (int i = 0; i < TILE_COUNT; i++)
        {
            Widget tile = client.getWidget(GROUP, FIRST_TILE + i);
            if (tile == null)
            {
                continue;
            }
            tile.setOriginalY(rowY(i % ROWS));
            tile.setOriginalHeight(ROW_PITCH);
            tile.revalidate();
        }

        int rowY = rowY(ROWS);                 // the new ninth row
        int rowH = root.getHeight() - rowY;
        int x0 = COL_X[0];

        // 2. Shrink the Total level strip into the two right-hand slots
        Widget total = client.getWidget(GROUP, TOTAL);
        if (total != null)
        {
            total.setOriginalX(COL_X[1]);
            total.setOriginalWidth(root.getWidth() - COL_X[1]);
            total.setOriginalY(rowY + (rowH - total.getOriginalHeight()) / 2);
            total.revalidate();
        }

        // 3. Build the Barding tile (flat children of the root; cropped with sprite tiling)
        addSprite(root, x0, rowY, 36, rowH, SPRITE_BG_LEFT, true);
        addSprite(root, x0 + 31, rowY, TILE_W - 31, rowH, SPRITE_BG_RIGHT, true);
        addSprite(root, x0 + 3, rowY + 2, 25, 25, SPRITE_PLACEHOLDER_ICON, false);
        addText(root, x0 + 32, rowY + 4, 15, 12, Integer.toString(level));
        addText(root, x0 + 44, rowY + 16, 15, 12, Integer.toString(level));
        addHitArea(root, x0, rowY, TILE_W, rowH);
    }

    /** Undo everything. Call on the client thread when the plugin shuts down. */
    public void reset()
    {
        for (Widget w : ours)
        {
            w.setHidden(true);
        }
        ours.clear();

        for (int i = 0; i < TILE_COUNT; i++)
        {
            Widget tile = client.getWidget(GROUP, FIRST_TILE + i);
            if (tile == null)
            {
                continue;
            }
            int row = i % ROWS;
            tile.setOriginalY(TOP + VANILLA_PITCH * row);
            tile.setOriginalHeight(row == ROWS - 1 ? VANILLA_LAST_ROW_HEIGHT : VANILLA_PITCH);
            tile.revalidate();
        }

        Widget total = client.getWidget(GROUP, TOTAL);
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

    private void addSprite(Widget parent, int x, int y, int w, int h, int spriteId, boolean crop)
    {
        Widget sprite = parent.createChild(-1, WidgetType.GRAPHIC);
        sprite.setSpriteId(spriteId);
        sprite.setSpriteTiling(crop); // tiling crops instead of stretching
        position(sprite, x, y, w, h);
        ours.add(sprite);
    }

    private void addText(Widget parent, int x, int y, int w, int h, String value)
    {
        Widget text = parent.createChild(-1, WidgetType.TEXT);
        text.setText(value);
        text.setFontId(FONT_STAT);
        text.setTextColor(COLOR_YELLOW);
        text.setTextShadowed(true);
        text.setXTextAlignment(1); // centered
        text.setYTextAlignment(1);
        position(text, x, y, w, h);
        ours.add(text);
    }

    private void addHitArea(Widget parent, int x, int y, int w, int h)
    {
        Widget hit = parent.createChild(-1, WidgetType.RECTANGLE);
        hit.setFilled(true);
        hit.setOpacity(255); // fully transparent
        hit.setHasListener(true);
        hit.setName("<col=ff9040>Barding</col>");
        hit.setAction(0, "View");
        hit.setOnOpListener((JavaScriptCallback) ev -> onClick.run());
        position(hit, x, y, w, h);
        ours.add(hit);
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