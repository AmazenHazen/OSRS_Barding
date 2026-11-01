package com.barding.data;

/**
 * What a riff does when played. Add a new value here when the design doc
 * introduces a new kind of effect, then handle it in the (future) riff service.
 *
 * Everything below PRAYER_DRAIN_PAUSE is described by Riff.description, including
 * numbers that differ between an instrument and its legendary version.
 */
public enum RiffEffect
{
	/** +N temporary boost to one or more skills for the player and nearby players. */
	SKILL_BOOST,
	/** Short stamina boost for the player. */
	STAMINA,
	/** Pauses prayer drain for a short time. */
	PRAYER_DRAIN_PAUSE,
	/** Distracts snakes while looting pots in Pyramid Plunder. */
	SNAKE_DISTRACT,
	/** Chance to give a nearby crop an extra Harvest Life. */
	HARVEST_LIFE,
	/** Chance to mature the ale in a completed Brewing Vat. */
	MATURE_ALE,
	/** Removes the player's skull outside PvP areas. */
	REMOVE_SKULL,
	/** Sends fossils from the inventory to fossil storage. */
	FOSSIL_TRANSPORT,
	/** Casts a stored Re-Animation spell charge. */
	CAST_REANIMATION,
	/** Stuns nearby enemies. */
	STUN,
	/** Increases crystal shard drops while skilling or killing monsters. */
	CRYSTAL_SHARD_BOOST,
	/** Chance to burn double logs for a short time. */
	DOUBLE_LOG_BURN,
	/** Drains the stats of nearby enemies. */
	STAT_DRAIN,
	/** Attracts hunter animals towards the player. */
	ATTRACT_HUNTER_ANIMALS,
	/** Smelts ore in the inventory using stored Superheat Item charges. */
	SUPERHEAT,
	/** Extends the next wind boost to the player's boat. */
	WIND_BOOST_EXTEND,
	/** Resets enemy aggression towards the player and briefly boosts Defence. */
	AGGRESSION_RESET,
	/** Slowly restores prayer for the player and nearby players. */
	PRAYER_RESTORE
}
