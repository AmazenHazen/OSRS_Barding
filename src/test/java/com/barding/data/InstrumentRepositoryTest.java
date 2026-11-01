package com.barding.data;

import com.google.gson.Gson;
import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Validates the data files. Run these whenever you edit instruments.json:
 * a typo in a data file should fail a test, not crash the plugin in-game.
 */
public class InstrumentRepositoryTest
{
	private final InstrumentRepository repo = InstrumentRepository.loadDefault(new Gson());

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
		}
	}

	@Test
	public void legendariesPointAtAStandardInstrument()
	{
		for (InstrumentDef def : repo.all())
		{
			if (!def.isLegendary())
			{
				assertNull(def.getId() + " is not legendary, so it should have no baseId", def.getBaseId());
				continue;
			}
			assertNotNull(def.getId() + " is legendary and needs a baseId", def.getBaseId());
			InstrumentDef base = repo.find(def.getBaseId()).orElse(null);
			assertNotNull(def.getId() + " has unknown baseId " + def.getBaseId(), base);
			assertFalse(def.getId() + " must point at a standard instrument, not another legendary", base.isLegendary());
			assertEquals(def.getId() + " should have the same level as " + base.getId(),
				base.getLevelRequired(), def.getLevelRequired());
			assertEquals(def.getId() + " should have the same type as " + base.getId(), base.getType(), def.getType());
			assertNull(def.getId() + " is won in duels, so it has no craft recipe", def.getCraft());
			assertNotNull(def.getId() + " needs a riff", def.getRiff());
		}
	}

	@Test(expected = IllegalStateException.class)
	public void duplicateIdsAreRejected()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"type\":\"RECORDER\"},{\"id\":\"a\",\"name\":\"B\",\"type\":\"RECORDER\"}]";
		InstrumentRepository.load(new StringReader(json), new Gson());
	}

	@Test
	public void enumValuesIgnoreCase()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"type\":\"recorder\"},{\"id\":\"b\",\"name\":\"B\",\"type\":\"Bells\"}]";
		InstrumentRepository loaded = InstrumentRepository.load(new StringReader(json), new Gson());
		assertEquals(InstrumentType.RECORDER, loaded.get("a").getType());
		assertEquals(InstrumentType.BELLS, loaded.get("b").getType());
	}

	@Test
	public void unknownEnumValueSaysWhatAndWhere()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"type\":\"RECORDER\"},{\"id\":\"b\",\"name\":\"B\",\"type\":\"Wnid\"}]";
		try
		{
			InstrumentRepository.load(new StringReader(json), new Gson());
			fail("an unknown type should be rejected");
		}
		catch (IllegalStateException e)
		{
			assertTrue(e.getMessage(), e.getMessage().contains("Wnid"));
			assertTrue(e.getMessage(), e.getMessage().contains("$[1].type"));
		}
	}

	@Test
	public void lookupByIdWorks()
	{
		assertEquals("Recorder", repo.get("recorder").getName());
		assertTrue(repo.get("recorder_of_guided_notes").isLegendary());
		assertFalse(repo.find("does_not_exist").isPresent());
	}

	private static void assertRiffValid(String owner, Riff riff)
	{
		assertNotNull(owner + " riff effect missing", riff.getEffect());
		assertTrue(owner + " bad duration", riff.getDurationSeconds() >= 0);
		assertTrue(owner + " bad cooldown", riff.getCooldownMinutes() >= 0);
		assertTrue(owner + " bad area", riff.getAreaSize() >= 1);
		switch (riff.getEffect())
		{
			case SKILL_BOOST:
				assertNotNull(owner + " skill boost needs skills", riff.getSkills());
				assertFalse(owner + " skill boost needs at least one skill", riff.getSkills().isEmpty());
				assertTrue(owner + " skill boost needs boost > 0", riff.getBoost() > 0);
				assertTrue(owner + " skill boost needs a duration", riff.getDurationSeconds() > 0);
				assertTrue(owner + " skill boost needs a cooldown", riff.getCooldownMinutes() > 0);
				break;
			case STAMINA:
			case PRAYER_DRAIN_PAUSE:
				assertTrue(owner + " needs a duration", riff.getDurationSeconds() > 0);
				assertTrue(owner + " needs a cooldown", riff.getCooldownMinutes() > 0);
				break;
			default:
				assertNotNull(owner + " riff needs a description", riff.getDescription());
				assertFalse(owner + " riff needs a description", riff.getDescription().isEmpty());
				break;
		}
	}
}
