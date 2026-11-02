package com.barding.progress;

import lombok.AllArgsConstructor;
import lombok.Data;

/** A player's own copy of an instrument. Saved as JSON, so renaming a field loses saved data. */
@Data
@AllArgsConstructor
public class InstrumentInstance
{
	/** Slot value for an instrument that is in storage rather than the inventory. */
	public static final int NOT_CARRIED = -1;

	/** The id of the InstrumentDef this is a copy of. */
	private final String instrumentId;

	/** How in tune it is, from 0 to 100. */
	private int tuning;

	/** The inventory slot it is drawn in, or NOT_CARRIED. */
	private int slot;

	public boolean isCarried()
	{
		return slot != NOT_CARRIED;
	}
}
