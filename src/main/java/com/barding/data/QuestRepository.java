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
 * Loads quests.json once and serves lookups by id.
 * Fails fast (IllegalStateException) on missing fields, unknown enum values or
 * duplicate ids, so bad data is caught the moment it is loaded.
 */
public class QuestRepository
{
	private static final String DEFAULT_RESOURCE = "quests.json";

	private final Map<String, QuestDef> byId;

	private QuestRepository(Map<String, QuestDef> byId)
	{
		this.byId = byId;
	}

	/** Loads quests.json from the classpath (src/main/resources/com/barding/data). */
	public static QuestRepository loadDefault(Gson gson)
	{
		try (InputStream in = QuestRepository.class.getResourceAsStream(DEFAULT_RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing resource: " + DEFAULT_RESOURCE);
			}
			return load(new InputStreamReader(in, StandardCharsets.UTF_8), gson);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not read " + DEFAULT_RESOURCE, e);
		}
	}

	/** Loads quests from any reader, which makes it easy to test with small inline JSON. */
	public static QuestRepository load(Reader reader, Gson gson)
	{
		Gson strict = gson.newBuilder()
			.registerTypeAdapterFactory(new StrictEnumAdapterFactory())
			.create();

		QuestDef[] defs;
		try
		{
			defs = strict.fromJson(reader, QuestDef[].class);
		}
		catch (JsonParseException e)
		{
			throw new IllegalStateException("Bad quest data: " + e.getMessage(), e);
		}
		if (defs == null)
		{
			throw new IllegalStateException("Quest data is empty");
		}

		Map<String, QuestDef> map = new LinkedHashMap<>();
		for (QuestDef def : defs)
		{
			if (def.getId() == null || def.getName() == null || def.getDifficulty() == null || def.getDescription() == null
				|| def.getSkillRequirements() == null || def.getQuestRequirements() == null || def.getRewards() == null)
			{
				throw new IllegalStateException("Quest is missing a required field: " + def);
			}
			if (map.put(def.getId(), def) != null)
			{
				throw new IllegalStateException("Duplicate quest id: " + def.getId());
			}
		}
		return new QuestRepository(Collections.unmodifiableMap(map));
	}

	public Optional<QuestDef> find(String id)
	{
		return Optional.ofNullable(byId.get(id));
	}

	public QuestDef get(String id)
	{
		return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown quest id: " + id));
	}

	/** All quests in the order they appear in the JSON file. */
	public Collection<QuestDef> all()
	{
		return byId.values();
	}

	public int size()
	{
		return byId.size();
	}
}
