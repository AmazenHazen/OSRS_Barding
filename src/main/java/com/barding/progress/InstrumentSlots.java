package com.barding.progress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Works out where carried instruments are drawn. They are not real items, so the game knows nothing
 * about them and can put a real item in the same slot at any time. Pure Java, so it can be tested.
 */
public final class InstrumentSlots
{
	public static final int INVENTORY_SIZE = 28;

	private InstrumentSlots()
	{
	}

	/**
	 * Keeps each carried instrument in its slot if it is still free, and moves the rest to the first
	 * empty slot. Returns the instruments that no longer fit, which have gone back to storage.
	 */
	public static List<InstrumentInstance> place(Collection<InstrumentInstance> instruments, boolean[] occupied)
	{
		boolean[] taken = occupied.clone();
		List<InstrumentInstance> moving = new ArrayList<>();
		for (InstrumentInstance instrument : instruments)
		{
			if (!instrument.isCarried())
			{
				continue;
			}
			int slot = instrument.getSlot();
			if (slot >= 0 && slot < taken.length && !taken[slot])
			{
				taken[slot] = true;
			}
			else
			{
				moving.add(instrument);
			}
		}

		List<InstrumentInstance> bumped = new ArrayList<>();
		for (InstrumentInstance instrument : moving)
		{
			int free = firstFree(taken);
			instrument.setSlot(free);
			if (free == InstrumentInstance.NOT_CARRIED)
			{
				bumped.add(instrument);
			}
			else
			{
				taken[free] = true;
			}
		}
		return bumped;
	}

	/** The first free slot, or NOT_CARRIED if there are none. */
	public static int firstFree(boolean[] taken)
	{
		for (int i = 0; i < taken.length; i++)
		{
			if (!taken[i])
			{
				return i;
			}
		}
		return InstrumentInstance.NOT_CARRIED;
	}
}
