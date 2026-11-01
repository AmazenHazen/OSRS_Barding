package com.barding.ui;

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
 * Turns an instrument id into its icon. The data files only hold the id; the picture lives in
 * src/main/resources/com/barding/icons/<id>.png. If a PNG is missing, a placeholder is drawn
 * (a dark tile with the first letter of the id), so the UI works before any art exists.
 */
@Singleton
public class InstrumentIcons
{
    public static final int SIZE = 28;
    private static final String PATH = "/com/barding/icons/";

    private final Map<String, BufferedImage> cache = new HashMap<>();

    @Inject
    public InstrumentIcons()
    {
    }

    public synchronized BufferedImage get(String instrumentId)
    {
        BufferedImage icon = cache.get(instrumentId);
        if (icon == null)
        {
            icon = load(instrumentId);
            cache.put(instrumentId, icon);
        }
        return icon;
    }

    private static BufferedImage load(String id)
    {
        String path = PATH + id + ".png";
        if (InstrumentIcons.class.getResource(path) != null)
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
        return placeholder(id);
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