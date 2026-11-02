package com.barding.progress;

import com.barding.data.InstrumentRepository;
import com.barding.data.InstrumentType;
import com.google.gson.Gson;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InstrumentSkillsTest
{
	private final Gson gson = new Gson();

	@Test
	public void everyoneStartsAsANovice()
	{
		assertEquals(InstrumentRank.NOVICE, InstrumentRank.of(0));
	}

	@Test
	public void ranksFollowTheSkillCurveEveryTenLevels()
	{
		assertEquals(1154, InstrumentRank.ADVANCED_BEGINNER.getMinXp());
		assertEquals(4470, InstrumentRank.COMPETENT.getMinXp());
		assertEquals(13363, InstrumentRank.PROFICIENT.getMinXp());
		assertEquals(37224, InstrumentRank.EXPERT.getMinXp());
	}

	@Test
	public void rankChangesExactlyAtItsThreshold()
	{
		assertEquals(InstrumentRank.NOVICE, InstrumentRank.of(1153));
		assertEquals(InstrumentRank.ADVANCED_BEGINNER, InstrumentRank.of(1154));
		assertEquals(InstrumentRank.PROFICIENT, InstrumentRank.of(37223));
		assertEquals(InstrumentRank.EXPERT, InstrumentRank.of(37224));
		assertEquals(InstrumentRank.EXPERT, InstrumentRank.of(200_000_000));
	}

	@Test
	public void ranksGoInOrder()
	{
		assertEquals(InstrumentRank.ADVANCED_BEGINNER, InstrumentRank.NOVICE.next());
		assertEquals(InstrumentRank.EXPERT, InstrumentRank.PROFICIENT.next());
		assertNull(InstrumentRank.EXPERT.next());
	}

	@Test
	public void progressIsMeasuredWithinTheCurrentRank()
	{
		assertEquals(0, InstrumentRank.progressPercent(0));
		assertEquals(50, InstrumentRank.progressPercent(577));
		assertEquals(99, InstrumentRank.progressPercent(1153));
		assertEquals(0, InstrumentRank.progressPercent(1154));
		assertEquals(100, InstrumentRank.progressPercent(37224));
		assertEquals(100, InstrumentRank.progressPercent(200_000_000));
	}

	@Test
	public void anInstrumentAndItsLegendaryShareExperience()
	{
		InstrumentRepository instruments = InstrumentRepository.loadDefault(gson);
		assertEquals(instruments.get("lute").getType(), instruments.get("lute_of_lost_harmonies").getType());
	}

	@Test
	public void savedExperienceIsReadBack()
	{
		Map<InstrumentType, Integer> xp = new EnumMap<>(InstrumentType.class);
		xp.put(InstrumentType.LUTE, 5000);
		xp.put(InstrumentType.FRAME_DRUM, 12);
		assertEquals(xp, InstrumentSkills.decode(InstrumentSkills.encode(xp, gson), gson));
	}

	@Test
	public void badSavedValuesCountAsNoExperience()
	{
		assertTrue(InstrumentSkills.decode(null, gson).isEmpty());
		assertTrue(InstrumentSkills.decode("", gson).isEmpty());
		assertTrue(InstrumentSkills.decode("not json", gson).isEmpty());
	}

	@Test
	public void aRemovedInstrumentTypeIsIgnored()
	{
		Map<InstrumentType, Integer> read = InstrumentSkills.decode("{\"LUTE\":5,\"KAZOO\":9}", gson);
		assertEquals(Map.of(InstrumentType.LUTE, 5), read);
	}

	@Test
	public void experienceIsCapped()
	{
		assertEquals(200_000_000, InstrumentSkills.add(199_999_999, Integer.MAX_VALUE));
	}
}
