package com.barding.data;

import lombok.Value;

/**
 * One musician the player can duel, as described in duels.json. This is pure data:
 * no RuneLite types, so it can be loaded and tested without a running client.
 *
 * The instrument needed for the duel is the legendary's baseId. "questId" and "song"
 * may be absent (null) for duels that need no extra quest or have no set song yet.
 */
@Value
public class DuelDef
{
	/** Stable snake_case key used for references and saved progress. Never change it once released. */
	String id;

	/** Display name shown in the skill guide. */
	String name;

	/** Barding level needed to challenge this musician. */
	int levelRequired;

	String location;

	/** Id of the legendary instrument this musician can reward. */
	String legendaryId;

	/** Id of a quest needed on top of A Noteworthy Duel. */
	String questId;

	String song;
}
