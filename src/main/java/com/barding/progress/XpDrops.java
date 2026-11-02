package com.barding.progress;

import com.barding.data.InstrumentDef;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import lombok.Value;

/**
 * Recent experience gains waiting to be shown on screen. Gains at nearly the same moment share a
 * drop, so one note's Barding and instrument experience appear side by side. Client thread only.
 */
@Singleton
public class XpDrops
{
	/** How long a drop stays on screen, in milliseconds. */
	public static final long LIFETIME = 1800;

	private static final long SAME_DROP_WINDOW = 100;

	/** One amount in a drop. The instrument is null for Barding experience. */
	@Value
	public static class Part
	{
		InstrumentDef instrument;
		int amount;
	}

	@Value
	public static class Drop
	{
		long startedAt;
		List<Part> parts;
	}

	private final List<Drop> drops = new ArrayList<>();

	public void add(InstrumentDef instrument, int amount)
	{
		add(instrument, amount, System.currentTimeMillis());
	}

	void add(InstrumentDef instrument, int amount, long now)
	{
		Drop last = drops.isEmpty() ? null : drops.get(drops.size() - 1);
		if (last == null || now - last.getStartedAt() > SAME_DROP_WINDOW)
		{
			last = new Drop(now, new ArrayList<>());
			drops.add(last);
		}
		last.getParts().add(new Part(instrument, amount));
	}

	/** The drops still on screen, oldest first. */
	public List<Drop> active(long now)
	{
		drops.removeIf(drop -> now - drop.getStartedAt() > LIFETIME);
		return drops;
	}
}
