package com.barding.data;

import com.google.gson.Gson;
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
 * Loads instruments.json once and serves lookups by id.
 * Fails fast (IllegalStateException) on missing fields or duplicate ids,
 * so bad data is caught the moment it is loaded.
 */
public class InstrumentRepository
{
	private static final String DEFAULT_RESOURCE = "instruments.json";

	private final Map<String, InstrumentDef> byId;

	private InstrumentRepository(Map<String, InstrumentDef> byId)
	{
		this.byId = byId;
	}

	/** Loads instruments.json from the classpath (src/main/resources/com/barding/data). */
	public static InstrumentRepository loadDefault()
	{
		try (InputStream in = InstrumentRepository.class.getResourceAsStream(DEFAULT_RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing resource: " + DEFAULT_RESOURCE);
			}
			return load(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not read " + DEFAULT_RESOURCE, e);
		}
	}

	/** Loads instruments from any reader, which makes it easy to test with small inline JSON. */
	public static InstrumentRepository load(Reader reader)
	{
		InstrumentDef[] defs = new Gson().fromJson(reader, InstrumentDef[].class);
		if (defs == null)
		{
			throw new IllegalStateException("Instrument data is empty");
		}

		Map<String, InstrumentDef> map = new LinkedHashMap<>();
		for (InstrumentDef def : defs)
		{
			if (def.getId() == null || def.getName() == null || def.getType() == null)
			{
				throw new IllegalStateException("Instrument is missing id, name or type: " + def);
			}
			if (map.put(def.getId(), def) != null)
			{
				throw new IllegalStateException("Duplicate instrument id: " + def.getId());
			}
		}
		return new InstrumentRepository(Collections.unmodifiableMap(map));
	}

	public Optional<InstrumentDef> find(String id)
	{
		return Optional.ofNullable(byId.get(id));
	}

	public InstrumentDef get(String id)
	{
		return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown instrument id: " + id));
	}

	/** All instruments in the order they appear in the JSON file. */
	public Collection<InstrumentDef> all()
	{
		return byId.values();
	}

	public int size()
	{
		return byId.size();
	}
}
