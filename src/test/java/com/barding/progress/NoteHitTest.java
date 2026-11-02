package com.barding.progress;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NoteHitTest
{
	private static final double DELTA = 0.0001;

	@Test
	public void freshInstrumentAtLevelOne()
	{
		assertEquals(30.144, NoteHit.chance(1, InstrumentRank.NOVICE, InstrumentStorage.UNLOCK_TUNING, NoteHit.STATIC_MASTERY), DELTA);
	}

	@Test
	public void aFullyTunedInstrumentHitsAtLeastHalfTheTime()
	{
		assertEquals(50.24, NoteHit.chance(1, InstrumentRank.NOVICE, 100, 0), DELTA);
		assertTrue(NoteHit.chance(1, InstrumentRank.NOVICE, 100, 0) >= 50);
	}

	@Test
	public void midGame()
	{
		assertEquals(70, NoteHit.chance(50, InstrumentRank.COMPETENT, 100, 0), DELTA);
		assertEquals(58.5, NoteHit.chance(50, InstrumentRank.EXPERT, 50, 0), DELTA);
	}

	@Test
	public void bestPlayerWithoutMastery()
	{
		assertEquals(89.76, NoteHit.chance(99, InstrumentRank.EXPERT, 100, 0), DELTA);
	}

	@Test
	public void bestPossibleWithMasteryStaysUnderOneHundred()
	{
		assertEquals(99.76, NoteHit.chance(99, InstrumentRank.EXPERT, 100, 10), DELTA);
	}

	@Test
	public void tuningAffectsEverything()
	{
		assertEquals(53.856, NoteHit.chance(99, InstrumentRank.EXPERT, 20, 0), DELTA);
	}

	@Test
	public void masteryIsTheSmallestFactor()
	{
		double best = NoteHit.chance(99, InstrumentRank.EXPERT, 100, 10);
		assertEquals(10, best - NoteHit.chance(99, InstrumentRank.EXPERT, 100, 0), DELTA);
		assertTrue(best - NoteHit.chance(99, InstrumentRank.NOVICE, 100, 10) > 10);
		assertTrue(best - NoteHit.chance(1, InstrumentRank.EXPERT, 100, 10) > 10);
		assertTrue(best - NoteHit.chance(99, InstrumentRank.EXPERT, 20, 10) > 10);
	}

	@Test
	public void rollsBelowTheChanceHit()
	{
		assertTrue(NoteHit.hits(24.18, 24.17));
		assertFalse(NoteHit.hits(24.18, 24.18));
		assertFalse(NoteHit.hits(24.18, 99.99));
	}
}
