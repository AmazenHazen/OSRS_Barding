package com.barding.data;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StationRepositoryTest
{
	private final List<StationDef> stations = StationRepository.loadDefault(new Gson());

	@Test
	public void stationsLoad()
	{
		assertEquals(3, stations.size());
	}

	@Test
	public void valuesAreInRange()
	{
		for (StationDef station : stations)
		{
			assertTrue("Bad id: " + station.getId(), station.getId().matches("[a-z0-9_]+"));
			assertTrue(station.getId() + " bad plane", station.getPlane() >= 0 && station.getPlane() <= 3);
			assertTrue(station.getId() + " bad orientation", station.getOrientation() >= 0 && station.getOrientation() < 2048);
		}
	}
}
