package com.barding.progress;

import com.barding.BardingConfig;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;

/**
 * The instruments a player has unlocked, saved per account in their RuneLite profile.
 * Everything that changes them runs on the client thread.
 */
@Slf4j
@Singleton
public class InstrumentStorage
{
	/** How in tune an instrument is when it is first unlocked. */
	public static final int UNLOCK_TUNING = 20;

	private static final String KEY = "instruments";
	private static final Type LIST_TYPE = new TypeToken<List<InstrumentInstance>>() { }.getType();

	private final Client client;
	private final ConfigManager configManager;
	private final Gson gson;

	private volatile Map<String, InstrumentInstance> owned = new LinkedHashMap<>();

	@Inject
	public InstrumentStorage(Client client, ConfigManager configManager, Gson gson)
	{
		this.client = client;
		this.configManager = configManager;
		this.gson = gson;
	}

	/** Reads the current account's instruments. Call on startup and whenever the account changes. */
	public void load()
	{
		Map<String, InstrumentInstance> loaded = new LinkedHashMap<>();
		for (InstrumentInstance instrument : decode(configManager.getRSProfileConfiguration(BardingConfig.GROUP, KEY), gson))
		{
			loaded.put(instrument.getInstrumentId(), instrument);
		}
		owned = loaded;
		log.debug("Loaded {} Barding instruments", loaded.size());
	}

	public Collection<InstrumentInstance> all()
	{
		return Collections.unmodifiableCollection(owned.values());
	}

	public Optional<InstrumentInstance> find(String instrumentId)
	{
		return Optional.ofNullable(owned.get(instrumentId));
	}

	/** Adds an instrument to storage. Does nothing if it is already unlocked. */
	public void unlock(String instrumentId)
	{
		if (owned.containsKey(instrumentId) || configManager.getRSProfileKey() == null)
		{
			return;
		}
		owned.put(instrumentId, new InstrumentInstance(instrumentId, UNLOCK_TUNING, InstrumentInstance.NOT_CARRIED));
		save();
	}

	public void setTuning(String instrumentId, int tuning)
	{
		find(instrumentId).ifPresent(instrument ->
		{
			instrument.setTuning(Math.max(0, Math.min(100, tuning)));
			save();
		});
	}

	/** Moves an instrument from storage to the first empty inventory slot. */
	public void take(String instrumentId)
	{
		InstrumentInstance instrument = owned.get(instrumentId);
		boolean[] occupied = occupiedSlots();
		if (instrument == null || instrument.isCarried() || occupied == null)
		{
			return;
		}
		InstrumentSlots.place(carried(), occupied);
		for (InstrumentInstance other : carried())
		{
			occupied[other.getSlot()] = true;
		}
		int free = InstrumentSlots.firstFree(occupied);
		if (free == InstrumentInstance.NOT_CARRIED)
		{
			message("You don't have enough inventory space to do that.");
			return;
		}
		instrument.setSlot(free);
		save();
	}

	/** Moves an instrument from the inventory back to storage. */
	public void store(String instrumentId)
	{
		find(instrumentId).ifPresent(instrument ->
		{
			instrument.setSlot(InstrumentInstance.NOT_CARRIED);
			save();
		});
	}

	/** Moves carried instruments out of the way of real items. Call when the inventory changes. */
	public void refreshSlots()
	{
		boolean[] occupied = occupiedSlots();
		List<InstrumentInstance> carried = carried();
		if (occupied == null || carried.isEmpty())
		{
			return;
		}
		if (!InstrumentSlots.place(carried, occupied).isEmpty())
		{
			message("Your inventory is full, so your instrument has gone back to storage.");
		}
		save();
	}

	/** Forgets everything. Only for testing. */
	public void reset()
	{
		owned.clear();
		save();
	}

	private List<InstrumentInstance> carried()
	{
		List<InstrumentInstance> carried = new ArrayList<>();
		for (InstrumentInstance instrument : owned.values())
		{
			if (instrument.isCarried())
			{
				carried.add(instrument);
			}
		}
		return carried;
	}

	/** Which slots hold real items, or null if the inventory has not loaded yet. */
	private boolean[] occupiedSlots()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return null;
		}
		boolean[] occupied = new boolean[InstrumentSlots.INVENTORY_SIZE];
		for (int i = 0; i < occupied.length; i++)
		{
			Item item = inventory.getItem(i);
			occupied[i] = item != null && item.getId() != -1;
		}
		return occupied;
	}

	private void save()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(BardingConfig.GROUP, KEY, encode(owned.values(), gson));
		}
	}

	private void message(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}

	static String encode(Collection<InstrumentInstance> instruments, Gson gson)
	{
		return gson.toJson(instruments, LIST_TYPE);
	}

	/** Reads saved instruments. Anything missing or unreadable counts as no instruments. */
	static List<InstrumentInstance> decode(String saved, Gson gson)
	{
		if (saved == null || saved.isEmpty())
		{
			return new ArrayList<>();
		}
		try
		{
			List<InstrumentInstance> decoded = gson.fromJson(saved, LIST_TYPE);
			return decoded == null ? new ArrayList<>() : decoded;
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read saved Barding instruments", e);
			return new ArrayList<>();
		}
	}
}
