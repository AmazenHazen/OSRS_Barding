package com.barding.data;

import com.google.gson.Gson;
import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Validates quests.json: a typo in a data file should fail a test, not crash the plugin in-game. */
public class QuestRepositoryTest
{
	private final QuestRepository repo = QuestRepository.loadDefault(new Gson());

	@Test
	public void loadsEveryQuest()
	{
		assertEquals(8, repo.size());
	}

	@Test
	public void idsAreSnakeCase()
	{
		for (QuestDef quest : repo.all())
		{
			assertTrue("Bad id: " + quest.getId(), quest.getId().matches("[a-z0-9_]+"));
		}
	}

	@Test
	public void everyQuestHasRewards()
	{
		for (QuestDef quest : repo.all())
		{
			assertFalse(quest.getId() + " has no rewards", quest.getRewards().isEmpty());
		}
	}

	@Test
	public void skillRequirementsAreValid()
	{
		for (QuestDef quest : repo.all())
		{
			for (SkillRequirement r : quest.getSkillRequirements())
			{
				assertTrue(quest.getId() + " bad level", r.getLevel() >= 1 && r.getLevel() <= 99);
			}
		}
	}

	@Test(expected = IllegalStateException.class)
	public void unknownDifficultyIsRejected()
	{
		String json = "[{\"id\":\"a\",\"name\":\"A\",\"difficulty\":\"EASY\",\"description\":\"D\","
			+ "\"skillRequirements\":[],\"questRequirements\":[],\"rewards\":[\"R\"]}]";
		QuestRepository.load(new StringReader(json), new Gson());
	}

	@Test(expected = IllegalStateException.class)
	public void missingFieldsAreRejected()
	{
		QuestRepository.load(new StringReader("[{\"id\":\"a\",\"name\":\"A\"}]"), new Gson());
	}
}
