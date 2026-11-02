package com.barding.progress;

/**
 * Builds up experience in hundredths, because saved experience is whole numbers. What hasn't reached
 * a whole point yet carries over to the next award, but isn't saved when the client closes.
 */
public final class FractionalXp
{
	private static final int HUNDREDTHS_PER_XP = 100;

	private int hundredths;

	/** Adds some hundredths of experience and returns the whole experience that is ready to award. */
	public int add(int amount)
	{
		hundredths += amount;
		int whole = hundredths / HUNDREDTHS_PER_XP;
		hundredths %= HUNDREDTHS_PER_XP;
		return whole;
	}

	/** Turns an experience value from the data files into hundredths. */
	public static int toHundredths(double xp)
	{
		return (int) Math.round(xp * HUNDREDTHS_PER_XP);
	}
}
