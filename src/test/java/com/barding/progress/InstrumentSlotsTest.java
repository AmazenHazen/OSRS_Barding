package com.barding.progress;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstrumentSlotsTest
{
	private static InstrumentInstance carriedIn(String id, int slot)
	{
		return new InstrumentInstance(id, InstrumentStorage.UNLOCK_TUNING, slot);
	}

	private static boolean[] occupied(int... slots)
	{
		boolean[] occupied = new boolean[InstrumentSlots.INVENTORY_SIZE];
		for (int slot : slots)
		{
			occupied[slot] = true;
		}
		return occupied;
	}

	private static boolean[] full()
	{
		boolean[] occupied = new boolean[InstrumentSlots.INVENTORY_SIZE];
		Arrays.fill(occupied, true);
		return occupied;
	}

	@Test
	public void instrumentStaysInAFreeSlot()
	{
		InstrumentInstance lute = carriedIn("lute", 5);
		assertTrue(InstrumentSlots.place(List.of(lute), occupied(0, 1)).isEmpty());
		assertEquals(5, lute.getSlot());
	}

	@Test
	public void realItemPushesInstrumentToFirstEmptySlot()
	{
		InstrumentInstance lute = carriedIn("lute", 1);
		assertTrue(InstrumentSlots.place(List.of(lute), occupied(0, 1)).isEmpty());
		assertEquals(2, lute.getSlot());
	}

	@Test
	public void movedInstrumentAvoidsOnesThatStayPut()
	{
		InstrumentInstance moved = carriedIn("lute", 0);
		InstrumentInstance stays = carriedIn("harp", 1);
		InstrumentSlots.place(List.of(moved, stays), occupied(0));
		assertEquals(1, stays.getSlot());
		assertEquals(2, moved.getSlot());
	}

	@Test
	public void noRoomSendsInstrumentBackToStorage()
	{
		InstrumentInstance lute = carriedIn("lute", 3);
		List<InstrumentInstance> bumped = InstrumentSlots.place(List.of(lute), full());
		assertEquals(List.of(lute), bumped);
		assertFalse(lute.isCarried());
	}

	@Test
	public void storedInstrumentsAreLeftAlone()
	{
		InstrumentInstance stored = carriedIn("lute", InstrumentInstance.NOT_CARRIED);
		assertTrue(InstrumentSlots.place(List.of(stored), full()).isEmpty());
		assertFalse(stored.isCarried());
	}

	@Test
	public void outOfRangeSlotIsMoved()
	{
		InstrumentInstance lute = carriedIn("lute", 99);
		InstrumentSlots.place(List.of(lute), occupied());
		assertEquals(0, lute.getSlot());
	}
}
