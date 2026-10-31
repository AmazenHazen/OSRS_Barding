package com.barding.ui;

import com.barding.data.InstrumentRepository;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/** The Barding sidebar panel: Overview / Instruments / Duels tabs. */
public class BardingPanel extends PluginPanel
{
    public BardingPanel(InstrumentRepository instruments)
    {
        super();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel display = new JPanel(new BorderLayout());
        display.setBackground(ColorScheme.DARK_GRAY_COLOR);

        MaterialTabGroup tabs = new MaterialTabGroup(display);
        tabs.setBorder(new EmptyBorder(5, 0, 7, 0));

        MaterialTab overview = new MaterialTab("Overview", tabs, placeholder("Skill overview and Quest XP coming soon."));
        MaterialTab instrumentTab = new MaterialTab("Instruments", tabs, new InstrumentsTab(instruments.all()));
        MaterialTab duels = new MaterialTab("Duels", tabs, placeholder("Musician Duels coming soon."));

        tabs.addTab(overview);
        tabs.addTab(instrumentTab);
        tabs.addTab(duels);
        tabs.select(instrumentTab);

        add(tabs, BorderLayout.NORTH);
        add(display, BorderLayout.CENTER);
    }

    private static JPanel placeholder(String text)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JLabel label = new JLabel("<html><body style='width:165px'>" + text + "</body></html>");
        label.setForeground(Color.WHITE);
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }
}