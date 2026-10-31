package com.barding;

import com.barding.data.InstrumentRepository;
import com.barding.ui.BardingPanel;
import com.barding.ui.SkillsTabTile;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Barding"
)
public class BardingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BardingConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SkillsTabTile skillsTabTile;

	private InstrumentRepository instruments;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		instruments = InstrumentRepository.loadDefault();

		navButton = NavigationButton.builder()
				.tooltip("Barding")
				.icon(createIcon())
				.priority(7)
				.panel(new BardingPanel(instruments))
				.build();
		clientToolbar.addNavigation(navButton);

		skillsTabTile.setOnClick(() ->
				SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton)));

		log.info("Barding started with {} instruments", instruments.size());
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		clientThread.invoke(skillsTabTile::reset);
		log.debug("Barding stopped!");
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		skillsTabTile.update();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Barding says " + config.greeting(), null);
		}
	}

	@Provides
	BardingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BardingConfig.class);
	}

	/** Placeholder sidebar icon (a gold music note), drawn in code so no image file is needed yet. */
	private static BufferedImage createIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(255, 215, 0));
		g.fillOval(2, 10, 7, 5);
		g.setStroke(new BasicStroke(2f));
		g.drawLine(8, 12, 8, 2);
		g.drawLine(8, 2, 13, 5);
		g.dispose();
		return img;
	}

}
