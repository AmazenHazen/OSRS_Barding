package com.barding.progress;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InstrumentStorageTest
{
	private final Gson gson = new Gson();

	@Test
	public void savedInstrumentsAreReadBack()
	{
		String saved = InstrumentStorage.encode(List.of(new InstrumentInstance("lute", 45, 3)), gson);
		List<InstrumentInstance> loaded = InstrumentStorage.decode(saved, gson);
		assertEquals(1, loaded.size());
		assertEquals("lute", loaded.get(0).getInstrumentId());
		assertEquals(45, loaded.get(0).getTuning());
		assertEquals(3, loaded.get(0).getSlot());
	}

	@Test
	public void badSavedValuesCountAsNoInstruments()
	{
		assertTrue(InstrumentStorage.decode(null, gson).isEmpty());
		assertTrue(InstrumentStorage.decode("", gson).isEmpty());
		assertTrue(InstrumentStorage.decode("not json", gson).isEmpty());
	}

	@Test
	public void tuningStatesFollowThePercentage()
	{
		assertEquals(TuningState.IN_TUNE, TuningState.of(100));
		assertEquals(TuningState.SLIGHTLY_OUT, TuningState.of(99));
		assertEquals(TuningState.SLIGHTLY_OUT, TuningState.of(60));
		assertEquals(TuningState.OUT, TuningState.of(59));
		assertEquals(TuningState.OUT, TuningState.of(30));
		assertEquals(TuningState.EXTREMELY_OUT, TuningState.of(InstrumentStorage.UNLOCK_TUNING));
		assertEquals(TuningState.EXTREMELY_OUT, TuningState.of(0));
	}
}
