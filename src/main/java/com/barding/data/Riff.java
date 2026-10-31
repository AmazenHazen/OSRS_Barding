package com.barding.data;

import lombok.Value;

/** The effect an instrument gives when the player plays a riff. */
@Value
public class Riff
{
	RiffEffect effect;

	/** Skill name (upper case, e.g. "COOKING"). Only used by SKILL_BOOST, otherwise absent. */
	String skill;

	/** Size of the boost. Only used by SKILL_BOOST, otherwise 0. */
	int boost;

	/** How long the effect lasts, in seconds. */
	int durationSeconds;

	/** Cooldown before the riff can be played again, in minutes. */
	int cooldownMinutes;

	/** Side length of the square area that is affected (1 = only the player). */
	int areaSize;
}
