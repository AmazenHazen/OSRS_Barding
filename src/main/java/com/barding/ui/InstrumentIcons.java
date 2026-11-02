package com.barding.ui;

import com.barding.data.InstrumentDef;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.util.ImageUtil;

/**
 * Turns an instrument into its icon. The data files only hold the file name; the picture lives in
 * src/main/resources/com/barding/data/icons. If an instrument has no icon, a placeholder is drawn
 * (a dark tile with the first letter of the id), so the UI works before any art exists.
 */
@Singleton
public class InstrumentIcons
{
    public static final int SIZE = 28;
    private static final String PATH = "/com/barding/data/icons/";
    private static final String SKILL_ICON = "lyre.png";

    private final Map<String, BufferedImage> cache = new HashMap<>();

    @Inject
    public InstrumentIcons()
    {
    }

    public synchronized BufferedImage get(InstrumentDef def)
    {
        BufferedImage icon = cache.get(def.getId());
        if (icon == null)
        {
            icon = load(def);
            cache.put(def.getId(), icon);
        }
        return icon;
    }

    /** The Barding skill icon, scaled to the given height. */
    public BufferedImage skillIcon(int height)
    {
        BufferedImage image = ImageUtil.loadImageResource(InstrumentIcons.class, PATH + SKILL_ICON);
        return ImageUtil.resizeImage(image, image.getWidth() * height / image.getHeight(), height, true);
    }

    /** The width of the skill icon at the given height. */
    public int skillIconWidth(int height)
    {
        return skillIcon(height).getWidth();
    }

    private static BufferedImage load(InstrumentDef def)
    {
        String path = PATH + def.getIcon();
        if (def.getIcon() != null && InstrumentIcons.class.getResource(path) != null)
        {
            try
            {
                BufferedImage image = ImageUtil.loadImageResource(InstrumentIcons.class, path);
                return ImageUtil.resizeImage(image, SIZE, SIZE, true);
            }
            catch (RuntimeException e)
            {
                // unreadable PNG: fall through to the placeholder
            }
        }
        return placeholder(def.getId());
    }

    private static BufferedImage placeholder(String id)
    {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x2B261C));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setColor(new Color(0x6E6350));
        g.drawRect(0, 0, SIZE - 1, SIZE - 1);
        g.setColor(new Color(0xFF981F));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        String letter = id == null || id.isEmpty() ? "?" : id.substring(0, 1).toUpperCase();
        g.drawString(letter, (SIZE - fm.stringWidth(letter)) / 2, (SIZE - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        return img;
    }
}