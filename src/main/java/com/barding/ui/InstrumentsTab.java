package com.barding.ui;

import com.barding.data.InstrumentDef;
import com.barding.data.Material;
import com.barding.data.RiffText;
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
        if (def.isLegendary())
        {
            sb.append("<font color='#ffd700'><b>").append(def.getName()).append("</b></font><br>");
        }
        else
        {
            sb.append("<b>").append(def.getName()).append("</b><br>");
        }
        sb.append("<font color='#9a9a9a'>Level ").append(def.getLevelRequired())
                .append(" - ").append(RiffText.capitalize(def.getType().getFamily().name())).append("</font><br>");

        if (def.getCraft() != null)
        {
            String mats = def.getCraft().getMaterials().stream()
                    .map((Material m) -> m.getQuantity() + "x " + m.getItem())
                    .collect(Collectors.joining(", "));
            String reqs = def.getCraft().getRequirements().stream()
                    .map((SkillRequirement r) -> r.getLevel() + " " + RiffText.capitalize(r.getSkill()))
                    .collect(Collectors.joining(" & "));
            sb.append("Craft: ").append(mats);
            if (!reqs.isEmpty())
            {
                sb.append(" (").append(reqs).append(")");
            }
            sb.append("<br>");
        }
        if (def.getRiff() != null)
        {
            sb.append("Riff: ").append(RiffText.describe(def.getRiff())).append("<br>");
        }
        return sb.append("</body></html>").toString();
    }
}
