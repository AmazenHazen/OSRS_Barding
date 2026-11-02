package com.barding.progress;

import lombok.Getter;
import net.runelite.api.Experience;

/**
 * How skilled a player is with a type of instrument. Ranks follow the normal skill curve, at
 * every ten levels: reaching Expert takes the experience of level 40 in a normal skill.
 */
@Getter
public enum InstrumentRank
{
	NOVICE("Novice", 1),
	ADVANCED_BEGINNER("Advanced Beginner", 10),
	COMPETENT("Competent", 20),
	PROFICIENT("Proficient", 30),
	EXPERT("Expert", 40);

	private final String label;
	private final int minXp;

	InstrumentRank(String label, int curveLevel)
	{
		this.label = label;
		this.minXp = Experience.getXpForLevel(curveLevel);
	}

	public static InstrumentRank of(int xp)
	{
		InstrumentRank rank = NOVICE;
		for (InstrumentRank candidate : values())
		{
			if (xp >= candidate.minXp)
			{
				rank = candidate;
			}
		}
		return rank;
	}

	/** How far through the current rank this experience is, from 0 to 100. Always 100 at Expert. */
	public static int progressPercent(int xp)
	{
		InstrumentRank rank = of(xp);
		InstrumentRank next = rank.next();
		if (next == null)
		{
			return 100;
		}
		return (int) (100L * (xp - rank.minXp) / (next.minXp - rank.minXp));
	}

	/** The rank after this one, or null at Expert. */
	public InstrumentRank next()
	{
		return this == EXPERT ? null : values()[ordinal() + 1];
	}
}
