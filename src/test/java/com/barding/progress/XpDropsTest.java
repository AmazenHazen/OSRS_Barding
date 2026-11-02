package com.barding.progress;

import com.barding.data.InstrumentDef;
import com.barding.data.InstrumentRepository;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XpDropsTest
{
	private final InstrumentDef lute = InstrumentRepository.loadDefault(new Gson()).get("lute");
	private final XpDrops drops = new XpDrops();

	@Test
	public void oneNoteBecomesOneDropWithBothParts()
	{
		drops.add(null, 4, 1000);
		drops.add(lute, 3, 1000);
		assertEquals(1, drops.active(1000).size());
		assertEquals(2, drops.active(1000).get(0).getParts().size());
		assertNull(drops.active(1000).get(0).getParts().get(0).getInstrument());
		assertEquals(3, drops.active(1000).get(0).getParts().get(1).getAmount());
	}

	@Test
	public void laterGainsAreSeparateDrops()
	{
		drops.add(null, 4, 1000);
		drops.add(null, 4, 2200);
		assertEquals(2, drops.active(2200).size());
	}

	@Test
	public void dropsDisappearAfterTheirLifetime()
	{
		drops.add(null, 4, 1000);
		assertEquals(1, drops.active(1000 + XpDrops.LIFETIME).size());
		assertTrue(drops.active(1001 + XpDrops.LIFETIME).isEmpty());
	}
}
