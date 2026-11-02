package com.barding.progress;

import lombok.Value;
import net.runelite.api.Experience;

/**
 * An amount of Barding experience, on the same level curve as every other OSRS skill.
 * Immutable, and always between 0 and the game's 200m cap.
 */
@Value
public class BardingXp
{
	public static final BardingXp NONE = new BardingXp(0);

	int xp;

	private BardingXp(int xp)
	{
		this.xp = xp;
	}

	public static BardingXp of(long xp)
	{
		return new BardingXp((int) Math.max(0, Math.min(Experience.MAX_SKILL_XP, xp)));
	}

	/** Reads a saved value. Anything missing or unreadable counts as no experience. */
	public static BardingXp parse(String saved)
	{
		if (saved == null)
		{
			return NONE;
		}
		try
		{
			return of(Long.parseLong(saved.trim()));
		}
		catch (NumberFormatException e)
		{
			return NONE;
		}
	}

	public BardingXp plus(int amount)
	{
		return of((long) xp + amount);
	}

	/** The real level, from 1 to 99. */
	public int getLevel()
	{
		return Math.min(Experience.MAX_REAL_LEVEL, Experience.getLevelForXp(xp));
	}

	/** Total experience needed for the next level. Only meaningful below level 99. */
	public int getNextLevelXp()
	{
		return Experience.getXpForLevel(getLevel() + 1);
	}

	public boolean isMaxLevel()
	{
		return getLevel() >= Experience.MAX_REAL_LEVEL;
	}
}
