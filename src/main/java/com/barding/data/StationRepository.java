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

/** Loads stations.json and checks it, so a typo in the data fails at startup instead of in-game. */
public final class StationRepository
{
	private static final String RESOURCE = "stations.json";

	private StationRepository()
	{
	}

	public static List<StationDef> loadDefault(Gson gson)
	{
		try (InputStream in = StationRepository.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing resource: " + RESOURCE);
			}
			StationDef[] defs = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), StationDef[].class);
			if (defs == null)
			{
				throw new IllegalStateException("Station data is empty");
			}
			Set<String> ids = new HashSet<>();
			for (StationDef def : defs)
			{
				if (def.getId() == null || def.getName() == null || !ids.add(def.getId()))
				{
					throw new IllegalStateException("Station has a missing or duplicate id: " + def);
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
