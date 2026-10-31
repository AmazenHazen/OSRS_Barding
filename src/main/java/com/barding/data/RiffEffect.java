package com.barding.data;

/**
 * What a riff does when played. Add a new value here when the design doc
 * introduces a new kind of effect, then handle it in the (future) riff service.
 */
public enum RiffEffect
{
	/** +N temporary boost to one skill for the player and nearby players. */
	SKILL_BOOST,
	/** Short stamina boost for the player. */
	STAMINA,
	/** Pauses prayer drain for a short time. */
	PRAYER_DRAIN_PAUSE
}
