package com.barding.data;

import com.google.gson.Gson;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Validates duels.json: a typo in a data file should fail a test, not crash the plugin in-game. */
public class DuelRepositoryTest
{
	private final InstrumentRepository instruments = InstrumentRepository.loadDefault(new Gson());
	private final QuestRepository quests = QuestRepository.loadDefault(new Gson());
	private final DuelRepository repo = DuelRepository.loadDefault(new Gson(), instruments, quests);

	@Test
	public void idsAreSnakeCase()
	{
		for (DuelDef duel : repo.all())
		{
			assertTrue("Bad id: " + duel.getId(), duel.getId().matches("[a-z0-9_]+"));
		}
	}

	@Test
	public void levelsAreInRange()
	{
		for (DuelDef duel : repo.all())
		{
			assertTrue(duel.getId() + " level out of range", duel.getLevelRequired() >= 1 && duel.getLevelRequired() <= 99);
		}
	}

	@Test
	public void everyLegendaryIsWonFromExactlyOneMusician()
	{
		Set<String> rewarded = new HashSet<>();
		for (DuelDef duel : repo.all())
		{
			assertTrue(duel.getLegendaryId() + " is rewarded twice", rewarded.add(duel.getLegendaryId()));
		}
		for (InstrumentDef def : instruments.all())
		{
			assertEquals(def.getId() + " duel coverage", def.isLegendary(), rewarded.contains(def.getId()));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void standardInstrumentIsRejected()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"location\":\"L\",\"legendaryId\":\"recorder\"}]";
		DuelRepository.load(new StringReader(json), new Gson(), instruments, quests);
	}

	@Test(expected = IllegalStateException.class)
	public void unknownQuestIsRejected()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"location\":\"L\","
			+ "\"legendaryId\":\"recorder_of_guided_notes\",\"questId\":\"nope\"}]";
		DuelRepository.load(new StringReader(json), new Gson(), instruments, quests);
	}

	@Test(expected = IllegalStateException.class)
	public void duplicateIdsAreRejected()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"location\":\"L\",\"legendaryId\":\"recorder_of_guided_notes\"},"
			+ "{\"id\":\"a\",\"name\":\"B\",\"location\":\"L\",\"legendaryId\":\"ancient_gnomic_bells\"}]";
		DuelRepository.load(new StringReader(json), new Gson(), instruments, quests);
	}
}
