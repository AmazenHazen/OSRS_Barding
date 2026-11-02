package com.barding.world;

import com.barding.BardingConfig;
import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.barding.data.StationDef;
import com.barding.progress.CraftCheck;
import com.barding.progress.InstrumentStorage;
import com.barding.progress.ProgressService;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;

/**
 * Musician's benches: the POH bench with lathe, placed in the world on this client only. Other
 * players can't see them and they don't block movement. Right-clicking one crafts instruments.
 * Runs on the client thread.
 */
@Slf4j
@Singleton
public class CraftStation
{
	/** The three models of ObjectID.POH_WORKBENCH_5, read from the game cache. The API doesn't expose them. */
	private static final int[] BENCH_MODELS = {12188, 10559, 12186};

	private static final String NAME = "<col=ffff>Musician's bench</col>";
	private static final String MOVES_KEY = "stationMoves";

	private static final int HALF_TILE = Perspective.LOCAL_TILE_SIZE / 2;
	private static final int QUARTER_TURN = 512;
	private static final int FULL_TURN = 2048;
	private static final int REACH = 3;
	private static final int MODEL_ATTEMPTS_BEFORE_WARNING = 500;

	private final Client client;
	private final ConfigManager configManager;
	private final Gson gson;
	private final ProgressService progress;
	private final InstrumentStorage storage;
	private InstrumentRepository instruments;

	private final List<Bench> benches = new ArrayList<>();
	private Model model;
	private int modelAttempts;
	private int lastPlane = -1;

	private static final class Bench
	{
		final String id;
		final String name;
		WorldPoint location;
		int orientation;
		RuneLiteObject object;

		Bench(StationDef def)
		{
			id = def.getId();
			name = def.getName();
			apply(def);
		}

		void apply(StationDef def)
		{
			location = new WorldPoint(def.getX(), def.getY(), def.getPlane());
			orientation = def.getOrientation();
		}

		StationDef toDef()
		{
			return new StationDef(id, name, location.getX(), location.getY(), location.getPlane(), orientation);
		}
	}

	@Inject
	public CraftStation(Client client, ConfigManager configManager, Gson gson, ProgressService progress, InstrumentStorage storage)
	{
		this.client = client;
		this.configManager = configManager;
		this.gson = gson;
		this.progress = progress;
		this.storage = storage;
	}

	public void setData(InstrumentRepository instruments, List<StationDef> stations)
	{
		this.instruments = instruments;
		benches.clear();
		for (StationDef station : stations)
		{
			benches.add(new Bench(station));
		}
	}

	/** Applies the placements saved for this account. Call before place(), and when the account changes. */
	public void load()
	{
		String saved = configManager.getRSProfileConfiguration(BardingConfig.GROUP, MOVES_KEY);
		if (saved == null)
		{
			return;
		}
		try
		{
			StationDef[] moves = gson.fromJson(saved, StationDef[].class);
			for (Bench bench : benches)
			{
				for (StationDef move : moves)
				{
					if (bench.id.equals(move.getId()))
					{
						bench.apply(move);
					}
				}
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read saved Barding station placements", e);
		}
	}

	/** Puts the benches in the loaded area into the world. Call after every scene load. */
	public void place()
	{
		WorldView world = client.getTopLevelWorldView();
		lastPlane = world.getPlane();
		if (model == null && !loadModel())
		{
			return;
		}
		for (Bench bench : benches)
		{
			place(bench, world);
		}
	}

	/** Call every client tick. Models load in the background, and ladders change the floor without a scene load. */
	public void update()
	{
		if (model == null || client.getTopLevelWorldView().getPlane() != lastPlane)
		{
			place();
		}
	}

	private void place(Bench bench, WorldView world)
	{
		if (bench.object == null)
		{
			bench.object = client.createRuneLiteObject();
			bench.object.setModel(model);
		}
		LocalPoint tile = LocalPoint.fromWorld(world, bench.location);
		if (tile == null || world.getPlane() != bench.location.getPlane())
		{
			bench.object.setActive(false);
			return;
		}
		// A bench is two tiles wide, so its centre is on the edge between them
		bench.object.setLocation(new LocalPoint(tile.getX() + HALF_TILE, tile.getY(), world), bench.location.getPlane());
		bench.object.setOrientation(bench.orientation);
		bench.object.setActive(true);
		log.debug("Placed the {} bench at {}", bench.name, bench.location);
	}

	private boolean loadModel()
	{
		ModelData[] parts = new ModelData[BENCH_MODELS.length];
		for (int i = 0; i < parts.length; i++)
		{
			parts[i] = client.loadModelData(BENCH_MODELS[i]);
			if (parts[i] == null)
			{
				if (++modelAttempts == MODEL_ATTEMPTS_BEFORE_WARNING)
				{
					log.warn("Bench model {} still hasn't loaded", BENCH_MODELS[i]);
				}
				return false;
			}
		}
		model = client.mergeModels(parts).light();
		log.debug("Bench model loaded after {} failed attempts", modelAttempts);
		return true;
	}

	/** Takes the benches out of the world. */
	public void remove()
	{
		for (Bench bench : benches)
		{
			if (bench.object != null)
			{
				bench.object.setActive(false);
			}
		}
	}

	/** For finding the right spot: moves the nearest bench to the player and saves it for this account. */
	public void moveNearestToPlayer()
	{
		Player player = client.getLocalPlayer();
		Bench bench = nearest(player);
		if (bench != null)
		{
			bench.location = player.getWorldLocation();
			moved(bench);
		}
	}

	/** For finding the right spot: turns the nearest bench a quarter turn and saves it for this account. */
	public void rotateNearest()
	{
		Bench bench = nearest(client.getLocalPlayer());
		if (bench != null)
		{
			bench.orientation = (bench.orientation + QUARTER_TURN) % FULL_TURN;
			moved(bench);
		}
	}

	private Bench nearest(Player player)
	{
		if (player == null)
		{
			return null;
		}
		WorldPoint from = player.getWorldLocation();
		return benches.stream().min(Comparator.comparingInt((Bench bench) -> from.distanceTo2D(bench.location))).orElse(null);
	}

	private void moved(Bench bench)
	{
		place();
		List<StationDef> placements = new ArrayList<>();
		for (Bench other : benches)
		{
			placements.add(other.toDef());
		}
		configManager.setRSProfileConfiguration(BardingConfig.GROUP, MOVES_KEY, gson.toJson(placements));
		WorldPoint at = bench.location;
		message(bench.name + " bench: " + at.getX() + ", " + at.getY() + ", plane " + at.getPlane() + ", orientation " + bench.orientation + ".");
	}

	/** Call once per frame while the right-click menu is being built. */
	public void addMenuOptions(Point mouse)
	{
		if (instruments == null)
		{
			return;
		}
		for (Bench bench : benches)
		{
			if (bench.object != null && bench.object.isActive() && isUnder(bench, mouse))
			{
				addOptions(bench);
				return;
			}
		}
	}

	private void addOptions(Bench bench)
	{
		// Just below the top entry, so left-click still walks: a plugin can't walk the player to the bench
		Menu menu = client.getMenu();
		MenuEntry craft = menu.createMenuEntry(Math.max(0, menu.getMenuEntries().length - 1))
			.setOption("Craft")
			.setTarget(NAME)
			.setType(MenuAction.RUNELITE);

		List<InstrumentDef> craftable = craftable();
		if (craftable.isEmpty())
		{
			craft.onClick(entry -> message("You've crafted everything you can at your Barding level."));
			return;
		}
		Map<String, Integer> inventory = inventoryCounts();
		Menu instrumentMenu = craft.createSubMenu();
		for (InstrumentDef def : craftable)
		{
			boolean ready = CraftCheck.missing(def, progress.getLevel(), this::skillLevel, inventory).isEmpty();
			instrumentMenu.createMenuEntry(-1)
				.setOption((ready ? "<col=ffffff>" : "<col=ff0000>") + def.getName() + "</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> craft(bench, def));
		}
	}

	private boolean isUnder(Bench bench, Point mouse)
	{
		WorldView world = client.getTopLevelWorldView();
		LocalPoint local = bench.object.getLocation();
		int height = Perspective.getTileHeight(client, local, bench.location.getPlane());
		Shape clickbox = Perspective.getClickbox(client, world, model, bench.orientation, local.getX(), local.getY(), height);
		return clickbox != null && clickbox.contains(mouse.getX(), mouse.getY());
	}

	/** Standard instruments the player has the level for and hasn't unlocked. Lowest level first in the menu. */
	private List<InstrumentDef> craftable()
	{
		List<InstrumentDef> craftable = new ArrayList<>();
		for (InstrumentDef def : instruments.all())
		{
			if (def.getCraft() != null && def.getLevelRequired() <= progress.getLevel() && !storage.find(def.getId()).isPresent())
			{
				craftable.add(def);
			}
		}
		craftable.sort(Comparator.comparingInt(InstrumentDef::getLevelRequired).reversed());
		return craftable;
	}

	private void craft(Bench bench, InstrumentDef def)
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldLocation().distanceTo(bench.location) > REACH)
		{
			message("You need to be next to the bench to do that.");
			return;
		}
		List<String> missing = CraftCheck.missing(def, progress.getLevel(), this::skillLevel, inventoryCounts());
		if (!missing.isEmpty())
		{
			message("You need " + String.join(", ", missing) + " to craft that.");
			return;
		}
		storage.unlock(def.getId());
		message("You craft a " + def.getName() + ". It has been added to your instrument storage.");
		storage.take(def.getId());
	}

	private int skillLevel(String skill)
	{
		return client.getRealSkillLevel(Skill.valueOf(skill));
	}

	/** How many of each item the player is carrying, by lower case name. Noted items don't count. */
	private Map<String, Integer> inventoryCounts()
	{
		Map<String, Integer> counts = new HashMap<>();
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return counts;
		}
		for (Item item : inventory.getItems())
		{
			if (item.getId() == -1)
			{
				continue;
			}
			ItemComposition definition = client.getItemDefinition(item.getId());
			if (definition.getNote() == -1)
			{
				counts.merge(definition.getName().toLowerCase(Locale.ROOT), item.getQuantity(), Integer::sum);
			}
		}
		return counts;
	}

	private void message(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}
}
