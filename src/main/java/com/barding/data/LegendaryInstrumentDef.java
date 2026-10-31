package com.barding.data;

import lombok.Value;

/** The legendary version of an instrument, won from a Musician Duel. */
@Value
public class LegendaryInstrumentDef
{
	String id;
	String name;

	/** The (stronger) riff this legendary instrument plays. */
	Riff riff;
}
