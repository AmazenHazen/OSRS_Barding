package com.barding.data;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Loads guide.json and checks it, so a typo in the data fails at startup instead of in-game. */
public final class GuideRepository
{
	private static final String RESOURCE = "guide.json";
	private static final Set<String> BLOCK_TYPES = Set.of("heading", "text", "bullets", "instruments", "legendaries", "duels", "quests");

	private GuideRepository()
	{
	}

	public static GuideData loadDefault(Gson gson)
	{
		try (InputStream in = GuideRepository.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("Missing resource: " + RESOURCE);
			}
			return load(new InputStreamReader(in, StandardCharsets.UTF_8), gson);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not read " + RESOURCE, e);
		}
	}

	public static GuideData load(Reader reader, Gson gson)
	{
		GuideData data = gson.fromJson(reader, GuideData.class);
		if (data == null || data.getPages() == null || data.getPages().isEmpty())
		{
			throw new IllegalStateException("guide.json has no pages");
		}
		for (GuidePage page : data.getPages())
		{
			if (page.getTitle() == null || page.getTabs() == null || page.getTabs().isEmpty())
			{
				throw new IllegalStateException("Every guide page needs a title and at least one tab: " + page);
			}
			for (GuideTab tab : page.getTabs())
			{
				if (tab.getTitle() == null || tab.getBlocks() == null)
				{
					throw new IllegalStateException("Every guide tab needs a title and blocks: " + tab);
				}
				for (GuideBlock block : tab.getBlocks())
				{
					if (block.getType() == null || !BLOCK_TYPES.contains(block.getType()))
					{
						throw new IllegalStateException("Unknown guide block type: " + block.getType());
					}
				}
			}
		}
		return data;
	}
}
