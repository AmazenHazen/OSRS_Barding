package com.barding.data;

import java.util.List;
import lombok.Value;

/** The effect an instrument gives when the player plays a riff. */
@Value
public class Riff
{
	RiffEffect effect;

	/** Skill names (upper case, e.g. ["RANGED", "DEFENCE"]). Only used by SKILL_BOOST, otherwise absent. */
	List<String> skills;

	/** Size of the boost. Only used by SKILL_BOOST, otherwise 0. */
	int boost;

	/** How long the effect lasts, in seconds. */
	int durationSeconds;

	/** Cooldown before the riff can be played again, in minutes. */
	int cooldownMinutes;

	/** Side length of the square area that is affected (1 = only the player). */
	int areaSize;

	/** What the riff does, in words. Absent for SKILL_BOOST, STAMINA and PRAYER_DRAIN_PAUSE. */
	String description;
}
