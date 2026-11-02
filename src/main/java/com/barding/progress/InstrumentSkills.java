package com.barding.progress;

import com.barding.BardingConfig;
import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentType;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.client.config.ConfigManager;

/**
 * Experience with each type of instrument, saved per account in the player's RuneLite profile.
 * An instrument and its legendary version share a type, so they share this experience.
 * Everything that changes it runs on the client thread.
 */
@Slf4j
@Singleton
public class InstrumentSkills
{
	private static final String KEY = "instrumentXp";
	private static final Type MAP_TYPE = new TypeToken<Map<InstrumentType, Integer>>() { }.getType();

	private final Client client;
	private final ConfigManager configManager;
	private final Gson gson;
	private final XpDrops drops;

	private volatile Map<InstrumentType, Integer> xp = new EnumMap<>(InstrumentType.class);

	@Inject
	public InstrumentSkills(Client client, ConfigManager configManager, Gson gson, XpDrops drops)
	{
		this.client = client;
		this.configManager = configManager;
		this.gson = gson;
		this.drops = drops;
	}

	/** Reads the current account's experience. Call on startup and whenever the account changes. */
	public void load()
	{
		xp = decode(configManager.getRSProfileConfiguration(BardingConfig.GROUP, KEY), gson);
	}

	public int getXp(InstrumentType type)
	{
		return xp.getOrDefault(type, 0);
	}

	public InstrumentRank getRank(InstrumentType type)
	{
		return InstrumentRank.of(getXp(type));
	}

	/** Adds experience and saves it, announcing a new rank. */
	public void addXp(InstrumentDef def, int amount)
	{
		if (amount <= 0 || configManager.getRSProfileKey() == null)
		{
			return;
		}
		InstrumentType type = def.getType();
		InstrumentRank oldRank = getRank(type);
		xp.put(type, add(getXp(type), amount));
		save();
		drops.add(def, amount);
		InstrumentRank newRank = getRank(type);
		if (newRank != oldRank)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"Congratulations, your skill with the " + def.getName() + " is now " + newRank.getLabel() + ".", null);
		}
	}

	/** Forgets everything. Only for testing. */
	public void reset()
	{
		xp.clear();
		save();
	}

	private void save()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(BardingConfig.GROUP, KEY, encode(xp, gson));
		}
	}

	/** Adds without overflowing, capped like every other kind of experience. */
	static int add(int xp, int amount)
	{
		return (int) Math.min(Experience.MAX_SKILL_XP, (long) xp + amount);
	}

	static String encode(Map<InstrumentType, Integer> xp, Gson gson)
	{
		return gson.toJson(xp, MAP_TYPE);
	}

	/** Reads saved experience. Anything missing or unreadable counts as none. */
	static Map<InstrumentType, Integer> decode(String saved, Gson gson)
	{
		Map<InstrumentType, Integer> decoded = new EnumMap<>(InstrumentType.class);
		if (saved == null || saved.isEmpty())
		{
			return decoded;
		}
		try
		{
			Map<InstrumentType, Integer> read = gson.fromJson(saved, MAP_TYPE);
			if (read != null)
			{
				read.remove(null); // a type that no longer exists
				decoded.putAll(read);
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read saved Barding instrument experience", e);
		}
		return decoded;
	}
}
