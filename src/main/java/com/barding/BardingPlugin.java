package com.barding;

import com.barding.data.DuelRepository;
import com.barding.data.GuideRepository;
import com.barding.data.InstrumentRepository;
import com.barding.data.PubDef;
import com.barding.data.PubRepository;
import com.barding.data.QuestRepository;
import com.barding.data.StationRepository;
import com.barding.progress.BardingXp;
import com.barding.progress.InstrumentSkills;
import com.barding.progress.InstrumentStorage;
import com.barding.progress.PlayService;
import com.barding.progress.ProgressService;
import com.barding.progress.TuningService;
import com.barding.ui.BardingPanel;
import com.barding.ui.GuideWindow;
import com.barding.ui.InstrumentTab;
import com.barding.ui.InventoryInstruments;
import com.barding.ui.LevelUpDialog;
import com.barding.ui.OpenMenus;
import com.barding.ui.PubAreaOverlay;
import com.barding.ui.SkillsTabTile;
import com.barding.ui.SoundLogger;
import com.barding.ui.XpDropOverlay;
import com.barding.world.CraftStation;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.QuantityFormatter;

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
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private SkillsTabTile skillsTabTile;

	@Inject
	private GuideWindow guideWindow;

	@Inject
	private OpenMenus openMenus;

	@Inject
	private Gson gson;

	@Inject
	private ProgressService progress;

	@Inject
	private InstrumentStorage storage;

	@Inject
	private InstrumentSkills instrumentSkills;

	@Inject
	private TuningService tuningService;

	@Inject
	private PlayService playService;

	@Inject
	private XpDropOverlay xpDropOverlay;

	@Inject
	private PubAreaOverlay pubAreaOverlay;

	@Inject
	private LevelUpDialog levelUpDialog;

	@Inject
	private SoundLogger soundLogger;

	@Inject
	private EventBus eventBus;

	private boolean showingPubAreas;
	private boolean loggingSounds;

	@Inject
	private InstrumentTab instrumentTab;

	@Inject
	private InventoryInstruments inventoryInstruments;

	@Inject
	private CraftStation craftStation;

	@Inject
	private TooltipManager tooltipManager;

	@Inject
	@Named("developerMode")
	private boolean developerMode;

	private InstrumentRepository instruments;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		instruments = InstrumentRepository.loadDefault(gson);
		progress.load();
		progress.setOnLevelUp(levelUpDialog::show);
		storage.load();
		instrumentSkills.load();
		instrumentTab.setInstruments(instruments);
		inventoryInstruments.setInstruments(instruments);
		List<PubDef> pubs = PubRepository.loadDefault(gson);
		playService.setPubs(pubs);
		pubAreaOverlay.setPubs(pubs);
		overlayManager.add(xpDropOverlay);
		craftStation.setData(instruments, StationRepository.loadDefault(gson));
		craftStation.load();
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				craftStation.place();
			}
		});
		overlayManager.add(instrumentTab);
		overlayManager.add(inventoryInstruments);
		mouseManager.registerMouseWheelListener(instrumentTab);

		// Sidebar panel (kept for now)
		navButton = NavigationButton.builder()
			.tooltip("Barding")
			.icon(createIcon())
			.priority(7)
			.panel(new BardingPanel(instruments))
			.build();
		clientToolbar.addNavigation(navButton);

		// In-game skill guide window, opened from the Barding tile in the Skills tab
		QuestRepository quests = QuestRepository.loadDefault(gson);
		DuelRepository duels = DuelRepository.loadDefault(gson, instruments, quests);
		guideWindow.setData(GuideRepository.loadDefault(gson), instruments, duels, quests);
		overlayManager.add(guideWindow);
		mouseManager.registerMouseListener(guideWindow);
		mouseManager.registerMouseWheelListener(guideWindow);
		skillsTabTile.setOnClick(() -> clientThread.invokeLater(this::toggleGuide));

		log.info("Barding started with {} instruments", instruments.size());
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);

		guideWindow.close();
		mouseManager.unregisterMouseListener(guideWindow);
		mouseManager.unregisterMouseWheelListener(guideWindow);
		overlayManager.remove(guideWindow);

		overlayManager.remove(instrumentTab);
		overlayManager.remove(inventoryInstruments);
		overlayManager.remove(xpDropOverlay);
		overlayManager.remove(pubAreaOverlay);
		showingPubAreas = false;
		if (loggingSounds)
		{
			eventBus.unregister(soundLogger);
			loggingSounds = false;
		}
		mouseManager.unregisterMouseWheelListener(instrumentTab);

		clientThread.invoke(skillsTabTile::reset);
		clientThread.invoke(instrumentTab::reset);
		clientThread.invoke(craftStation::remove);
		clientThread.invoke(playService::stop);
		log.info("Barding stopped!");
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		skillsTabTile.update();
		instrumentTab.update();
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			craftStation.update();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		tuningService.tick();
		playService.tick();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		progress.load();
		storage.load();
		storage.refreshSlots();
		instrumentSkills.load();
		craftStation.load();
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			craftStation.place();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			storage.refreshSlots();
		}
	}

	/** Runs once per frame, after the game has built the menu for whatever is under the mouse. */
	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!client.isMenuOpen())
		{
			Point mouse = client.getMouseCanvasPosition();
			instrumentTab.addMenuOptions(mouse);
			inventoryInstruments.addMenuOptions(mouse);
			craftStation.addMenuOptions(mouse);
		}
	}

	/** Tooltips only last one frame, so this is added again every frame while hovering. */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (skillsTabTile.isHovered())
		{
			tooltipManager.add(new Tooltip(xpTooltip(progress.getXp())));
		}
	}

	private static String xpTooltip(BardingXp xp)
	{
		String text = "Barding XP: " + QuantityFormatter.formatNumber(xp.getXp());
		if (xp.isMaxLevel())
		{
			return text;
		}
		return text + "</br>Next level at: " + QuantityFormatter.formatNumber(xp.getNextLevelXp())
			+ "</br>Remaining XP: " + QuantityFormatter.formatNumber(xp.getNextLevelXp() - xp.getXp());
	}

	/** Test commands, for developer builds only: ::bardingxp 1000 adds experience, ::bardingxp reset clears it. */
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!developerMode)
		{
			return;
		}
		if ("bardingpubs".equals(event.getCommand()))
		{
			togglePubAreas();
			return;
		}
		if ("bardingsounds".equals(event.getCommand()))
		{
			toggleSoundLogging();
			return;
		}
		// ::bardingstation moves the nearest craft bench to the player, ::bardingstation rotate turns it
		if ("bardingstation".equals(event.getCommand()))
		{
			if (event.getArguments().length == 0)
			{
				craftStation.moveNearestToPlayer();
			}
			else
			{
				craftStation.rotateNearest();
			}
			return;
		}
		if (event.getArguments().length == 0)
		{
			return;
		}
		if ("bardinginstrument".equals(event.getCommand()))
		{
			instrumentCommand(event.getArguments());
		}
		else if ("bardinginstrumentxp".equals(event.getCommand()))
		{
			instrumentXpCommand(event.getArguments());
		}
		else if ("bardingxp".equals(event.getCommand()))
		{
			xpCommand(event.getArguments()[0]);
		}
	}

	/** ::bardingsounds prints every sound the game plays, for finding sound ids. */
	private void toggleSoundLogging()
	{
		loggingSounds = !loggingSounds;
		if (loggingSounds)
		{
			eventBus.register(soundLogger);
		}
		else
		{
			eventBus.unregister(soundLogger);
		}
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Sound logging " + (loggingSounds ? "on" : "off") + ".", null);
	}

	/** ::bardingpubs highlights the tiles that count as pubs, and says which pub the player is in. */
	private void togglePubAreas()
	{
		showingPubAreas = !showingPubAreas;
		if (showingPubAreas)
		{
			overlayManager.add(pubAreaOverlay);
		}
		else
		{
			overlayManager.remove(pubAreaOverlay);
		}
		Player player = client.getLocalPlayer();
		PubDef pub = player == null ? null : playService.pubAt(player.getWorldLocation());
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Pub tiles " + (showingPubAreas ? "shown" : "hidden") + ". You are "
			+ (pub == null ? "not in a pub" : "in " + pub.getName()) + ".", null);
	}

	/** ::bardinginstrument lute unlocks it, adding a number sets its tuning, ::bardinginstrument reset clears them all. */
	private void instrumentCommand(String[] arguments)
	{
		String id = arguments[0];
		if ("reset".equals(id))
		{
			storage.reset();
			return;
		}
		if (!instruments.find(id).isPresent())
		{
			log.debug("Not an instrument: {}", id);
			return;
		}
		storage.unlock(id);
		if (arguments.length > 1)
		{
			try
			{
				storage.setTuning(id, Integer.parseInt(arguments[1]));
			}
			catch (NumberFormatException e)
			{
				log.debug("Not a tuning percentage: {}", arguments[1]);
			}
		}
	}

	/** ::bardinginstrumentxp lute 5000 adds experience, ::bardinginstrumentxp reset clears all of it. */
	private void instrumentXpCommand(String[] arguments)
	{
		if ("reset".equals(arguments[0]))
		{
			instrumentSkills.reset();
			return;
		}
		if (arguments.length < 2 || !instruments.find(arguments[0]).isPresent())
		{
			log.debug("Expected an instrument and an amount of xp");
			return;
		}
		try
		{
			instrumentSkills.addXp(instruments.get(arguments[0]), Integer.parseInt(arguments[1]));
		}
		catch (NumberFormatException e)
		{
			log.debug("Not an amount of xp: {}", arguments[1]);
		}
	}

	private void xpCommand(String argument)
	{
		if ("reset".equals(argument))
		{
			progress.reset();
			return;
		}
		try
		{
			progress.addXp(Integer.parseInt(argument));
		}
		catch (NumberFormatException e)
		{
			log.debug("Not an amount of xp: {}", argument);
		}
	}

	/** Any other menu opening closes the guide. Checked a moment later, once the game has registered the menu. */
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (guideWindow.isVisible())
		{
			int groupId = event.getGroupId();
			clientThread.invokeLater(() ->
			{
				if (openMenus.isModal(groupId))
				{
					guideWindow.close();
				}
			});
		}
	}

	private void toggleGuide()
	{
		if (!guideWindow.isVisible())
		{
			openMenus.closeSkillGuides();
		}
		guideWindow.toggle();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			craftStation.place();
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Barding says " + config.greeting(), null);
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			guideWindow.close();
			tuningService.stop();
			playService.stop();
		}
	}

	@Provides
	BardingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BardingConfig.class);
	}

	/** Placeholder sidebar icon (a gold music note), drawn in code. */
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
