package com.barding.progress;

/**
 * Whether a played note is hit. The chance is out of 100:
 * (50 + 0.24 * Barding level + 4 * rank + song mastery) * (0.5 + tuning / 200)
 * A fully tuned instrument always has at least a 50% chance. Pure Java, so it can be tested.
 */
public final class NoteHit
{
	/** Song mastery bonus until songs exist. It will run from 0 up to 10, the smallest of the factors. */
	public static final int STATIC_MASTERY = 0;

	private static final double BASE_CHANCE = 50;
	private static final double PER_BARDING_LEVEL = 0.24;
	private static final double PER_RANK = 4;

	private NoteHit()
	{
	}

	public static double chance(int bardingLevel, InstrumentRank rank, int tuning, int mastery)
	{
		double skill = BASE_CHANCE + PER_BARDING_LEVEL * bardingLevel + PER_RANK * rank.ordinal() + mastery;
		return skill * (0.5 + tuning / 200.0);
	}

	/** True if a roll from 0 up to (but not including) 100 hits at this chance. */
	public static boolean hits(double chance, double roll)
	{
		return roll < chance;
	}
}
