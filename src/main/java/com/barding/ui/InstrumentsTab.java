package com.barding.ui;

import com.barding.data.InstrumentDef;
import com.barding.data.Material;
import com.barding.data.Riff;
import com.barding.data.SkillRequirement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

/** Skill guide tab: one card per instrument, built entirely from instruments.json. */
class InstrumentsTab extends JPanel
{
    InstrumentsTab(Collection<InstrumentDef> instruments)
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(ColorScheme.DARK_GRAY_COLOR);

        instruments.stream()
                .sorted(Comparator.comparingInt(InstrumentDef::getLevelRequired))
                .forEach(def ->
                {
                    list.add(createCard(def));
                    list.add(Box.createVerticalStrut(5));
                });

        add(list, BorderLayout.NORTH);
    }

    private static JPanel createCard(InstrumentDef def)
    {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel label = new JLabel(cardHtml(def));
        label.setForeground(Color.WHITE);
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    private static String cardHtml(InstrumentDef def)
    {
        StringBuilder sb = new StringBuilder("<html><body style='width:165px'>");
        sb.append("<b>").append(def.getName()).append("</b><br>");
        sb.append("<font color='#9a9a9a'>Level ").append(def.getLevelRequired())
                .append(" - ").append(capitalize(def.getType().name())).append("</font><br>");

        if (def.getCraft() != null)
        {
            String mats = def.getCraft().getMaterials().stream()
                    .map((Material m) -> m.getQuantity() + "x " + m.getItem())
                    .collect(Collectors.joining(", "));
            String reqs = def.getCraft().getRequirements().stream()
                    .map((SkillRequirement r) -> r.getLevel() + " " + capitalize(r.getSkill()))
                    .collect(Collectors.joining(" & "));
            sb.append("Craft: ").append(mats).append(" (").append(reqs).append(")<br>");
        }
        if (def.getRiff() != null)
        {
            sb.append("Riff: ").append(describeRiff(def.getRiff())).append("<br>");
        }
        if (def.getLegendary() != null)
        {
            sb.append("<font color='#ffd700'>Legendary: ")
                    .append(def.getLegendary().getName()).append("</font>");
        }
        return sb.append("</body></html>").toString();
    }

    private static String describeRiff(Riff riff)
    {
        String cooldown = " (" + riff.getCooldownMinutes() + " min cooldown)";
        switch (riff.getEffect())
        {
            case SKILL_BOOST:
                String area = riff.getAreaSize() > 1 ? ", " + riff.getAreaSize() + "x" + riff.getAreaSize() + " area" : "";
                return "+" + riff.getBoost() + " " + capitalize(riff.getSkill())
                        + " for " + (riff.getDurationSeconds() / 60) + " min" + area + cooldown;
            case STAMINA:
                return "Stamina boost, " + riff.getDurationSeconds() + "s" + cooldown;
            case PRAYER_DRAIN_PAUSE:
                return "Pauses prayer drain, " + riff.getDurationSeconds() + "s" + cooldown;
            default:
                return riff.getEffect().name();
        }
    }

    private static String capitalize(String s)
    {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
