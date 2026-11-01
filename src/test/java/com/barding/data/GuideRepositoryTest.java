package com.barding.data;

import com.google.gson.Gson;
import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuideRepositoryTest
{
	@Test
	public void guideLoadsAndHasPages()
	{
		GuideData data = GuideRepository.loadDefault(new Gson());
		assertFalse(data.getPages().isEmpty());
		for (GuidePage page : data.getPages())
		{
			assertTrue(page.getTitle() + " has no tabs", page.getTabs().size() >= 1);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void unknownBlockTypeIsRejected()
	{
		String json = "{\"pages\":[{\"title\":\"A\",\"tabs\":[{\"title\":\"T\",\"blocks\":[{\"type\":\"nope\"}]}]}]}";
		GuideRepository.load(new StringReader(json), new Gson());
	}
}
