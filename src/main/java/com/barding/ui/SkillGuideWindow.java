package com.barding.ui;

import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;

import net.runelite.api.MenuAction;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/**
 * Opens the game's own skill guide window (group 860) and fills it with Barding content.
 * Widget ids come from the Widget Inspector dump.
 */
@Slf4j
@Singleton
public class SkillGuideWindow
{
    private static final int GUIDE = 860;
    private static final int TITLE_LAYER = 3;      // 860.3, dynamic child 1 = title text
    private static final int TITLE_TEXT = 1;
    private static final int ROWS_LAYER = 7;       // 860.7, 9 dynamic children per category row
    private static final int ROW_SIZE = 9;
    private static final int ROW_LABEL = 8;        // the label is the 9th child of a row
    private static final int CONTENT_VIEW = 11;    // 860.11, the content area
    private static final int[] VANILLA_CONTENT = {14, 15, 19}; // Herblore's own content layers

    private static final int SKILLS_TAB = 320;
    private static final int HOST_TILE = 11;       // 320.11 (Herblore tile) opens the real guide
    private static final int OPEN_OP = 2;          // "View Herblore guide"

    private static final int FONT_BODY = 495;
    private static final int COLOR_BODY = 0xFF981F;

    private static final String[] PAGES = {"Overview", "Instruments", "Duels"};

    private final Client client;
    private InstrumentRepository instruments;

    private boolean active;      // true while this window is showing Barding content
    private int openTimeout;     // client ticks to wait for the window to appear
    private int page;
    private boolean dirty;
    private boolean wired;
    private boolean openRequested;
    private Widget content;

    @Inject
    public SkillGuideWindow(Client client)
    {
        this.client = client;
    }

    public void setInstruments(InstrumentRepository instruments)
    {
        this.instruments = instruments;
    }

    /** Fires the Herblore tile's own click handler, which opens the real window. Client thread only. */
    /** Called from the tile's click callback. Only sets a flag, because running a script from inside a script callback is not safe. */
    public void open()
    {
        openRequested = true;
    }

    private void launch()
    {
        // The tile has no client-side click handler: the server opens the guide.
        // So we send the same click a player makes on the Herblore tile.
        Widget tile = client.getWidget(SKILLS_TAB, HOST_TILE);
        if (tile == null)
        {
            log.info("BDBG launch: Herblore tile not found");
            return;
        }
        client.menuAction(-1, (SKILLS_TAB << 16) | HOST_TILE, MenuAction.CC_OP, OPEN_OP, -1,
                "View <col=ff981f>Herblore</col> guide", "");
        log.info("BDBG launch: replayed the Herblore tile click");

        page = 0;
        dirty = true;
        wired = false;
        content = null;
        active = true;
        openTimeout = 300; // the server has to answer first
    }

    /** Call every client tick. */
    public void update()
    {
        if (openRequested)
        {
            openRequested = false;
            launch();
        }
        if (!active)
        {
            return;
        }
        Widget root = client.getWidget(GUIDE, 0);
        if (root == null)
        {
            if (openTimeout > 0)
            {
                openTimeout--; // still waiting for the window to appear
            }
            else
            {
                log.info("BDBG guide window closed or never appeared");
                active = false;
                content = null;
                wired = false;
            }
            return;
        }
        openTimeout = 0;
        apply();
    }

    public void reset()
    {
        active = false;
        wired = false;
        if (content != null)
        {
            content.setHidden(true);
            content = null;
        }
    }

    private void select(int newPage)
    {
        page = newPage;
        dirty = true;
    }

    private void apply()
    {
        // Title
        Widget titleLayer = client.getWidget(GUIDE, TITLE_LAYER);
        if (titleLayer != null)
        {
            Widget[] title = titleLayer.getDynamicChildren();
            if (title != null && title.length > TITLE_TEXT && title[TITLE_TEXT] != null)
            {
                title[TITLE_TEXT].setText("Barding - " + PAGES[page] + " ");
            }
        }

        // Left-hand category list: relabel our rows, hide the rest
        Widget rows = client.getWidget(GUIDE, ROWS_LAYER);
        if (rows != null)
        {
            Widget[] kids = rows.getDynamicChildren();
            if (kids != null)
            {
                for (int r = 0; r * ROW_SIZE + ROW_LABEL < kids.length; r++)
                {
                    boolean used = r < PAGES.length;
                    for (int c = 0; c < ROW_SIZE; c++)
                    {
                        Widget w = kids[r * ROW_SIZE + c];
                        if (w != null)
                        {
                            w.setHidden(!used);
                        }
                    }
                    if (!used)
                    {
                        continue;
                    }
                    Widget label = kids[r * ROW_SIZE + ROW_LABEL];
                    if (label != null)
                    {
                        label.setText(PAGES[r]);
                    }
                    Widget hit = kids[r * ROW_SIZE];
                    if (!wired && hit != null)
                    {
                        final int target = r;
                        hit.setHasListener(true);
                        hit.setOnOpListener((JavaScriptCallback) ev -> select(target));
                    }
                }
                wired = true;
            }
        }

        // Hide Herblore's content, show ours
        for (int child : VANILLA_CONTENT)
        {
            Widget w = client.getWidget(GUIDE, child);
            if (w != null)
            {
                w.setHidden(true);
            }
        }
        Widget view = client.getWidget(GUIDE, CONTENT_VIEW);
        if (view == null)
        {
            return;
        }
        if (content == null)
        {
            content = view.createChild(-1, WidgetType.TEXT);
            content.setFontId(FONT_BODY);
            content.setTextColor(COLOR_BODY);
            content.setTextShadowed(false);
            content.setXTextAlignment(0);
            content.setYTextAlignment(0);
            content.setOriginalX(4);
            content.setOriginalY(4);
            content.setOriginalWidth(Math.max(200, view.getWidth() - 28));
            content.setOriginalHeight(300);
            dirty = true;
        }
        if (dirty)
        {
            content.setText(pageText(page));
            content.setHidden(false);
            content.revalidate();
            dirty = false;
        }
    }

    private String pageText(int p)
    {
        switch (p)
        {
            case 0:
                return "<col=ffffff>Barding</col> is a skill about performing music.<br><br>"
                        + "This window is a work in progress. Its text will come from data files.<br><br>"
                        + "Use the tabs on the left to browse.";
            case 1:
                return instrumentsText();
            default:
                return "Musician Duels are coming soon.";
        }
    }

    private String instrumentsText()
    {
        if (instruments == null)
        {
            return "No instrument data loaded.";
        }
        List<InstrumentDef> list = new ArrayList<>(instruments.all());
        list.sort(Comparator.comparingInt(InstrumentDef::getLevelRequired));
        StringBuilder sb = new StringBuilder();
        for (InstrumentDef def : list)
        {
            sb.append("<col=ffffff>").append(def.getLevelRequired()).append("</col>  ")
                    .append(def.getName()).append("<br>");
        }
        return sb.toString();
    }
}