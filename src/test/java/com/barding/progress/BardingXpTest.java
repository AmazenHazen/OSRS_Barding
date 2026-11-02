package com.barding.progress;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BardingXpTest
{
	@Test
	public void noExperienceIsLevelOne()
	{
		assertEquals(1, BardingXp.NONE.getLevel());
		assertEquals(83, BardingXp.NONE.getNextLevelXp());
	}

	@Test
	public void followsTheOsrsLevelCurve()
	{
		assertEquals(1, BardingXp.of(82).getLevel());
		assertEquals(2, BardingXp.of(83).getLevel());
		assertEquals(10, BardingXp.of(1154).getLevel());
		assertEquals(50, BardingXp.of(101333).getLevel());
		assertEquals(92, BardingXp.of(6517253).getLevel());
		assertEquals(98, BardingXp.of(13034430).getLevel());
		assertEquals(99, BardingXp.of(13034431).getLevel());
	}

	@Test
	public void levelStopsAtNinetyNine()
	{
		BardingXp max = BardingXp.of(200_000_000);
		assertEquals(99, max.getLevel());
		assertTrue(max.isMaxLevel());
		assertFalse(BardingXp.of(13034430).isMaxLevel());
	}

	@Test
	public void experienceIsCappedLikeOtherSkills()
	{
		assertEquals(200_000_000, BardingXp.of(199_999_999).plus(1000).getXp());
		assertEquals(0, BardingXp.of(-5).getXp());
	}

	@Test
	public void addingNearTheCapDoesNotOverflow()
	{
		assertEquals(200_000_000, BardingXp.of(200_000_000).plus(Integer.MAX_VALUE).getXp());
	}

	@Test
	public void savedValuesAreReadBack()
	{
		assertEquals(1234, BardingXp.parse("1234").getXp());
		assertEquals(1234, BardingXp.parse(" 1234 ").getXp());
	}

	@Test
	public void badSavedValuesCountAsNoExperience()
	{
		assertEquals(0, BardingXp.parse(null).getXp());
		assertEquals(0, BardingXp.parse("").getXp());
		assertEquals(0, BardingXp.parse("lots").getXp());
	}
}
