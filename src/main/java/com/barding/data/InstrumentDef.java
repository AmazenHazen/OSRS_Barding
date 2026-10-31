package com.barding.data;

import lombok.Value;

/**
 * One instrument, as described in instruments.json. This is pure data:
 * no RuneLite types, so it can be loaded and tested without a running client.
 *
 * "craft", "riff" and "legendary" may be absent (null) for instruments that
 * cannot be crafted, have no riff, or have no legendary version.
 */
@Value
public class InstrumentDef
{
	/** Stable snake_case key used for references and saved progress. Never change it once released. */
	String id;

	/** Display name shown in the skill guide. */
	String name;

	InstrumentType type;

	/** Barding level needed to use the instrument. */
	int levelRequired;

	CraftRecipe craft;
	Riff riff;
	LegendaryInstrumentDef legendary;
}
