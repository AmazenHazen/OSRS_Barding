package com.barding.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads duels.json once and serves lookups by id.
 * Fails fast (IllegalStateException) on missing fields, duplicate ids or references to
 * unknown instruments and quests, so bad data is caught the moment it is loaded.
 */
public class DuelRepository
{
	private static final String DEFAULT_RESOURCE = "duels.json";

	private final Map<String, DuelDef> byId;

	private DuelRepository(Map<String, DuelDef> byId)
	{
		this.byId = byId;
	}

	/** Loads duels.json from the classpath (src/main/resources/com/barding/data). */
	public static DuelRepository loadDefault(Gson gson, InstrumentRepository instruments, QuestRepository quests)
	{
		try (InputStream in = DuelRepository.class.getResourceAsStream(DEFAULT_RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing resource: " + DEFAULT_RESOURCE);
			}
			return load(new InputStreamReader(in, StandardCharsets.UTF_8), gson, instruments, quests);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not read " + DEFAULT_RESOURCE, e);
		}
	}

	/** Loads duels from any reader, which makes it easy to test with small inline JSON. */
	public static DuelRepository load(Reader reader, Gson gson, InstrumentRepository instruments, QuestRepository quests)
	{
		DuelDef[] defs;
		try
		{
			defs = gson.fromJson(reader, DuelDef[].class);
		}
		catch (JsonParseException e)
		{
			throw new IllegalStateException("Bad duel data: " + e.getMessage(), e);
		}
		if (defs == null)
		{
			throw new IllegalStateException("Duel data is empty");
		}

		Map<String, DuelDef> map = new LinkedHashMap<>();
		for (DuelDef def : defs)
		{
			if (def.getId() == null || def.getName() == null || def.getLocation() == null || def.getLegendaryId() == null)
			{
				throw new IllegalStateException("Duel is missing a required field: " + def);
			}
			InstrumentDef legendary = instruments.find(def.getLegendaryId()).orElse(null);
			if (legendary == null || !legendary.isLegendary())
			{
				throw new IllegalStateException(def.getId() + " rewards unknown legendary: " + def.getLegendaryId());
			}
			if (def.getQuestId() != null && !quests.find(def.getQuestId()).isPresent())
			{
				throw new IllegalStateException(def.getId() + " needs unknown quest: " + def.getQuestId());
			}
			if (map.put(def.getId(), def) != null)
			{
				throw new IllegalStateException("Duplicate duel id: " + def.getId());
			}
		}
		return new DuelRepository(Collections.unmodifiableMap(map));
	}

	public Optional<DuelDef> find(String id)
	{
		return Optional.ofNullable(byId.get(id));
	}

	public DuelDef get(String id)
	{
		return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown duel id: " + id));
	}

	/** All duels in the order they appear in the JSON file. */
	public Collection<DuelDef> all()
	{
		return byId.values();
	}

	public int size()
	{
		return byId.size();
	}
}
