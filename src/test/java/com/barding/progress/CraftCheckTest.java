package com.barding.progress;

import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.barding.data.Material;
import com.barding.data.SkillRequirement;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CraftCheckTest
{
	private final InstrumentRepository instruments = InstrumentRepository.loadDefault(new Gson());
	private final InstrumentDef recorder = instruments.get("recorder");
	private final InstrumentDef rebec = instruments.get("rebec");

	@Test
	public void nothingMissingWithLevelsAndMaterials()
	{
		assertTrue(CraftCheck.missing(recorder, 1, skill -> 1, Map.of("logs", 1)).isEmpty());
	}

	@Test
	public void missingBardingLevelIsReported()
	{
		assertEquals(List.of("10 Barding"), CraftCheck.missing(rebec, 9, skill -> 15, Map.of("logs", 3, "sinew", 3)));
	}

	@Test
	public void missingSkillLevelIsReported()
	{
		assertEquals(List.of("1 Fletching"), CraftCheck.missing(recorder, 1, skill -> 0, Map.of("logs", 1)));
	}

	@Test
	public void tooFewMaterialsIsReported()
	{
		assertEquals(List.of("3x Logs"), CraftCheck.missing(rebec, 10, skill -> 15, Map.of("logs", 2, "sinew", 3)));
	}

	@Test
	public void anyCrystalSeedCountsForTheCrystalFlute()
	{
		InstrumentDef flute = instruments.get("crystal_flute");
		for (String seed : List.of("crystal weapon seed", "crystal teleport seed", "crystal tool seed"))
		{
			assertTrue(seed, CraftCheck.missing(flute, 72, skill -> 72, Map.of(seed, 1, "crystal shard", 20)).isEmpty());
		}
		assertEquals(List.of("1x Crystal seed (any)"),
			CraftCheck.missing(flute, 72, skill -> 72, Map.of("crystal seed", 1, "crystal shard", 20)));
	}

	@Test
	public void symbolsNeedAllFourBronzeBars()
	{
		InstrumentDef symbols = instruments.get("symbols");
		assertEquals(List.of("4x Bronze bar"), CraftCheck.missing(symbols, 68, skill -> 66, Map.of("bronze bar", 2)));
		assertTrue(CraftCheck.missing(symbols, 68, skill -> 66, Map.of("bronze bar", 4)).isEmpty());
	}

	/** Materials are checked one by one, so an item listed twice would only need the larger amount. */
	@Test
	public void noRecipeListsAnItemTwice()
	{
		for (InstrumentDef def : instruments.all())
		{
			if (def.getCraft() != null)
			{
				Set<String> seen = new HashSet<>();
				for (Material material : def.getCraft().getMaterials())
				{
					assertTrue(def.getId() + " lists " + material.getItem() + " twice", seen.add(material.getItem()));
				}
			}
		}
	}

	@Test
	public void everyCraftSkillIsARealSkill()
	{
		for (InstrumentDef def : instruments.all())
		{
			if (def.getCraft() != null)
			{
				for (SkillRequirement requirement : def.getCraft().getRequirements())
				{
					Skill.valueOf(requirement.getSkill());
				}
			}
		}
	}
}
