package com.barding.data;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Validates the data files. Run these whenever you edit instruments.json:
 * a typo in a data file should fail a test, not crash the plugin in-game.
 */
public class InstrumentRepositoryTest
{
	private final InstrumentRepository repo = InstrumentRepository.loadDefault();

	@Test
	public void loadsInstruments()
	{
		assertTrue("instruments.json should not be empty", repo.size() > 0);
	}

	@Test
	public void idsAreSnakeCase()
	{
		for (InstrumentDef def : repo.all())
		{
			assertTrue("Bad id: " + def.getId(), def.getId().matches("[a-z0-9_]+"));
		}
	}

	@Test
	public void levelsAreInRange()
	{
		for (InstrumentDef def : repo.all())
		{
			assertTrue(def.getId() + " level out of range",
				def.getLevelRequired() >= 1 && def.getLevelRequired() <= 99);
		}
	}

	@Test
	public void craftRecipesAreValid()
	{
		for (InstrumentDef def : repo.all())
		{
			CraftRecipe craft = def.getCraft();
			if (craft == null)
			{
				continue;
			}
			assertFalse(def.getId() + " has no materials", craft.getMaterials().isEmpty());
			for (Material m : craft.getMaterials())
			{
				assertTrue(def.getId() + " bad material quantity", m.getQuantity() > 0);
			}
			for (SkillRequirement r : craft.getRequirements())
			{
				assertTrue(def.getId() + " bad craft level", r.getLevel() >= 1 && r.getLevel() <= 99);
			}
		}
	}

	@Test
	public void riffsAreValid()
	{
		for (InstrumentDef def : repo.all())
		{
			if (def.getRiff() != null)
			{
				assertRiffValid(def.getId(), def.getRiff());
			}
			if (def.getLegendary() != null)
			{
				assertRiffValid(def.getLegendary().getId(), def.getLegendary().getRiff());
			}
		}
	}

	@Test
	public void legendaryIdsAreUniqueAndDoNotCollide()
	{
		Set<String> seen = new HashSet<>();
		for (InstrumentDef def : repo.all())
		{
			seen.add(def.getId());
		}
		for (InstrumentDef def : repo.all())
		{
			if (def.getLegendary() != null)
			{
				assertTrue("Duplicate or colliding id: " + def.getLegendary().getId(),
					seen.add(def.getLegendary().getId()));
			}
		}
	}

	@Test(expected = IllegalStateException.class)
	public void duplicateIdsAreRejected()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"type\":\"WIND\"},{\"id\":\"a\",\"name\":\"B\",\"type\":\"WIND\"}]";
		InstrumentRepository.load(new StringReader(json));
	}

	@Test
	public void lookupByIdWorks()
	{
		assertEquals("Recorder", repo.get("recorder").getName());
		assertFalse(repo.find("does_not_exist").isPresent());
	}

	private static void assertRiffValid(String owner, Riff riff)
	{
		assertNotNull(owner + " riff missing", riff);
		assertNotNull(owner + " riff effect missing", riff.getEffect());
		assertTrue(owner + " bad duration", riff.getDurationSeconds() > 0);
		assertTrue(owner + " bad cooldown", riff.getCooldownMinutes() > 0);
		assertTrue(owner + " bad area", riff.getAreaSize() >= 1);
		if (riff.getEffect() == RiffEffect.SKILL_BOOST)
		{
			assertNotNull(owner + " skill boost needs a skill", riff.getSkill());
			assertTrue(owner + " skill boost needs boost > 0", riff.getBoost() > 0);
		}
	}
}
