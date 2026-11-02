package com.barding.progress;

import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TuningServiceTest
{
	private static final int TICKS_PER_HOUR = 6000;

	/** 30,000 experience an hour of non-stop tuning. This keeps a data typo from going past that. */
	private static final double MAX_TUNING_XP = 5;

	private final InstrumentRepository instruments = InstrumentRepository.loadDefault(new Gson());

	@Test
	public void tuningRisesOnePercentAtATime()
	{
		assertEquals(21, TuningService.nextTuning(InstrumentStorage.UNLOCK_TUNING));
		assertEquals(100, TuningService.nextTuning(99));
	}

	@Test
	public void tuningStopsAtOneHundred()
	{
		assertEquals(100, TuningService.nextTuning(100));
	}

	@Test
	public void xpPerPercentBecomesHundredths()
	{
		assertEquals(25, TuningService.hundredthsPerTick(0.25));
		assertEquals(420, TuningService.hundredthsPerTick(4.2));
	}

	@Test
	public void recorderGivesOneXpEveryFourPercent()
	{
		FractionalXp xp = new FractionalXp();
		int perTick = TuningService.hundredthsPerTick(instruments.get("recorder").getTuningXp());
		assertEquals(0, xp.add(perTick));
		assertEquals(0, xp.add(perTick));
		assertEquals(0, xp.add(perTick));
		assertEquals(1, xp.add(perTick));
	}

	@Test
	public void fullTuneOfAFreshRecorderGivesTwentyXp()
	{
		assertEquals(20, fullTune(instruments.get("recorder")));
	}

	@Test
	public void higherLevelInstrumentsCannotBeTuned()
	{
		InstrumentDef harp = instruments.get("harp");
		assertFalse(TuningService.hasLevelFor(harp, 90));
		assertTrue(TuningService.hasLevelFor(harp, 91));
		assertTrue(TuningService.hasLevelFor(harp, 99));
	}

	@Test
	public void fullTuneOfAFreshHarpGivesItsShare()
	{
		assertEquals(309, fullTune(instruments.get("harp")));
	}

	@Test
	public void bestInstrumentsTopOutBetweenTwentyAndThirtyThousandXpAnHour()
	{
		double perHour = instruments.get("magic_cithara").getTuningXp() * TICKS_PER_HOUR;
		assertTrue(perHour >= 20_000 && perHour <= 30_000);
	}

	@Test
	public void everyInstrumentHasUsableTuningXp()
	{
		for (InstrumentDef def : instruments.all())
		{
			assertTrue(def.getId() + " needs tuning xp", def.getTuningXp() > 0);
			assertEquals(def.getId() + " tuning xp must be in steps of 0.01",
				FractionalXp.toHundredths(def.getTuningXp()) / 100.0, def.getTuningXp(), 0.0001);
			assertTrue(def.getId() + " tuning xp is above the low cap", def.getTuningXp() <= MAX_TUNING_XP);
		}
	}

	@Test
	public void legendariesTuneForTheSameXp()
	{
		for (InstrumentDef def : instruments.all())
		{
			if (def.isLegendary())
			{
				assertEquals(def.getId(), instruments.get(def.getBaseId()).getTuningXp(), def.getTuningXp(), 0.0001);
			}
		}
	}

	@Test
	public void leftoverHundredthsCarryOver()
	{
		FractionalXp xp = new FractionalXp();
		assertEquals(9, xp.add(910));
		assertEquals(9, xp.add(910));
		assertEquals(0, xp.add(79));
		assertEquals(1, xp.add(1));
	}

	private static int fullTune(InstrumentDef def)
	{
		FractionalXp xp = new FractionalXp();
		int gained = 0;
		for (int tuning = InstrumentStorage.UNLOCK_TUNING; tuning < 100; tuning = TuningService.nextTuning(tuning))
		{
			gained += xp.add(TuningService.hundredthsPerTick(def.getTuningXp()));
		}
		return gained;
	}
}
