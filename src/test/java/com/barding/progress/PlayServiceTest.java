package com.barding.progress;

import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayServiceTest
{
	private static final int NOTES_PER_HOUR = 3000;
	private static final double DELTA = 0.0001;
	private static final double MAX_STEP = 1.7;

	private final InstrumentRepository instruments = InstrumentRepository.loadDefault(new Gson());

	@Test
	public void standardInstrumentsDriftOnOneNoteInTwenty()
	{
		assertTrue(PlayService.drifts(false, 4.99));
		assertFalse(PlayService.drifts(false, 5));
	}

	@Test
	public void legendaryInstrumentsDriftTwentyPercentSlower()
	{
		assertTrue(PlayService.drifts(true, 3.99));
		assertFalse(PlayService.drifts(true, 4));
	}

	/** The climb should have no plateaus or cliffs: each new instrument pays a bit more than the last. */
	@Test
	public void xpRisesSmoothlyWithInstrumentLevel()
	{
		InstrumentDef previous = null;
		for (InstrumentDef def : sortedStandardInstruments())
		{
			if (previous != null && def.getLevelRequired() > previous.getLevelRequired())
			{
				assertTrue(def.getId() + " play xp should rise", def.getPlayXp() > previous.getPlayXp());
				assertTrue(def.getId() + " tuning xp should rise", def.getTuningXp() > previous.getTuningXp());
				assertTrue(def.getId() + " play xp jumps too far", def.getPlayXp() <= previous.getPlayXp() * MAX_STEP);
				assertTrue(def.getId() + " tuning xp jumps too far", def.getTuningXp() <= previous.getTuningXp() * MAX_STEP);
			}
			previous = def;
		}
	}

	@Test
	public void instrumentsAtTheSameLevelPayTheSame()
	{
		assertEquals(instruments.get("rebec").getPlayXp(), instruments.get("organ").getPlayXp(), DELTA);
		assertEquals(instruments.get("symbols").getPlayXp(), instruments.get("fremennik_bodhran").getPlayXp(), DELTA);
	}

	@Test
	public void everyInstrumentHasUsablePlayXp()
	{
		for (InstrumentDef def : instruments.all())
		{
			assertTrue(def.getId() + " needs play xp", def.getPlayXp() > 0);
			assertEquals(def.getId() + " play xp must be in steps of 0.01",
				FractionalXp.toHundredths(def.getPlayXp()) / 100.0, def.getPlayXp(), DELTA);
		}
	}

	@Test
	public void legendariesPlayForTheSameXp()
	{
		for (InstrumentDef def : instruments.all())
		{
			if (def.isLegendary())
			{
				assertEquals(def.getId(), instruments.get(def.getBaseId()).getPlayXp(), def.getPlayXp(), DELTA);
			}
		}
	}

	@Test
	public void recorderGivesOneBardingXpPerHit()
	{
		assertEquals(1, instruments.get("recorder").getPlayXp(), DELTA);
		assertEquals(1, instruments.get("recorder_of_guided_notes").getPlayXp(), DELTA);
	}

	@Test
	public void recorderInstrumentXpBuildsUpFromFractions()
	{
		int perHit = FractionalXp.toHundredths(instruments.get("recorder").getPlayXp() * PlayService.INSTRUMENT_XP_SHARE);
		assertEquals(75, perHit);
		FractionalXp xp = new FractionalXp();
		assertEquals(0, xp.add(perHit));
		assertEquals(1, xp.add(perHit));
		assertEquals(1, xp.add(perHit));
		assertEquals(1, xp.add(perHit));
	}

	@Test
	public void playingATopInstrumentBeatsTuningIt()
	{
		InstrumentDef best = instruments.get("magic_cithara");
		double chance = NoteHit.chance(99, InstrumentRank.EXPERT, 100, NoteHit.STATIC_MASTERY);
		double playingPerHour = best.getPlayXp() * NOTES_PER_HOUR * chance / 100;
		double tuningPerHour = best.getTuningXp() * 6000;
		assertTrue(playingPerHour > tuningPerHour);
	}

	private List<InstrumentDef> sortedStandardInstruments()
	{
		List<InstrumentDef> sorted = new ArrayList<>();
		for (InstrumentDef def : instruments.all())
		{
			if (!def.isLegendary())
			{
				sorted.add(def);
			}
		}
		sorted.sort(Comparator.comparingInt(InstrumentDef::getLevelRequired));
		return sorted;
	}
}
