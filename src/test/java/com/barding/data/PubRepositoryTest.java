package com.barding.data;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PubRepositoryTest
{
	private final List<PubDef> pubs = PubRepository.loadDefault(new Gson());

	private PubDef blueMoon()
	{
		return pubs.stream().filter(pub -> pub.getId().equals("blue_moon_inn")).findFirst().orElseThrow();
	}

	@Test
	public void pubsLoad()
	{
		assertEquals(54, pubs.size());
	}

	@Test
	public void upperFloorPubsKeepTheirFloor()
	{
		PubDef paramaya = pubs.stream().filter(pub -> pub.getId().equals("paramaya_inn")).findFirst().orElseThrow();
		assertTrue(paramaya.contains(2860, 2997, 1));
		assertFalse(paramaya.contains(2860, 2997, 0));
	}

	@Test
	public void idsAreSnakeCase()
	{
		for (PubDef pub : pubs)
		{
			assertTrue("Bad id: " + pub.getId(), pub.getId().matches("[a-z0-9_]+"));
		}
	}

	@Test
	public void cornersCountAsInside()
	{
		assertTrue(blueMoon().contains(3216, 3393, 0));
		assertTrue(blueMoon().contains(3234, 3404, 0));
	}

	@Test
	public void outsideThePubDoesNotCount()
	{
		assertFalse(blueMoon().contains(3215, 3393, 0));
		assertFalse(blueMoon().contains(3234, 3405, 0));
	}

	@Test
	public void otherFloorsDoNotCount()
	{
		assertFalse(blueMoon().contains(3226, 3398, 1));
	}
}
