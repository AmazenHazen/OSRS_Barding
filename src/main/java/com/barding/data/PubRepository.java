package com.barding.data;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads pubs.json and checks it, so a typo in the data fails at startup instead of in-game. */
public final class PubRepository
{
	private static final String RESOURCE = "pubs.json";

	private PubRepository()
	{
	}

	public static List<PubDef> loadDefault(Gson gson)
	{
		try (InputStream in = PubRepository.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing resource: " + RESOURCE);
			}
			PubDef[] defs = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), PubDef[].class);
			if (defs == null)
			{
				throw new IllegalStateException("Pub data is empty");
			}
			Set<String> ids = new HashSet<>();
			for (PubDef def : defs)
			{
				if (def.getId() == null || def.getName() == null || !ids.add(def.getId())
					|| def.getMinX() > def.getMaxX() || def.getMinY() > def.getMaxY())
				{
					throw new IllegalStateException("Bad pub: " + def);
				}
			}
			return Arrays.asList(defs);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not read " + RESOURCE, e);
		}
	}
}
