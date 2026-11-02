package com.barding.progress;

import com.barding.data.InstrumentDef;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Tuning a carried instrument, which works anywhere and takes a little time. Runs on game ticks
 * and gives a little experience as it goes. Moving away stops it. Client thread only.
 */
@Singleton
public class TuningService
{
	/** How much in tune an instrument gets each game tick. */
	static final int TUNING_PER_TICK = 1;

	private final Client client;
	private final InstrumentStorage storage;
	private final ProgressService progress;
	private final InstrumentSkills skills;
	private final FractionalXp xp = new FractionalXp();

	private InstrumentDef tuning;
	private WorldPoint startedAt;

	@Inject
	public TuningService(Client client, InstrumentStorage storage, ProgressService progress, InstrumentSkills skills)
	{
		this.client = client;
		this.storage = storage;
		this.progress = progress;
		this.skills = skills;
	}

	public boolean canTune(int tuningPercent)
	{
		return tuningPercent < 100;
	}

	public boolean isTuning(InstrumentDef def)
	{
		return tuning != null && tuning.getId().equals(def.getId());
	}

	public void start(InstrumentDef def)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		if (!hasLevelFor(def, progress.getLevel()))
		{
			message("You need a Barding level of " + def.getLevelRequired() + " to tune that.");
			return;
		}
		tuning = def;
		startedAt = player.getWorldLocation();
		message("You begin tuning your " + def.getName() + ".");
	}

	public void stop()
	{
		tuning = null;
		startedAt = null;
	}

	/** Call every game tick. */
	public void tick()
	{
		if (tuning == null)
		{
			return;
		}
		InstrumentInstance instrument = storage.find(tuning.getId()).orElse(null);
		Player player = client.getLocalPlayer();
		if (instrument == null || !instrument.isCarried() || player == null)
		{
			stop();
			return;
		}
		if (!player.getWorldLocation().equals(startedAt))
		{
			message("You stop tuning your " + tuning.getName() + ".");
			stop();
			return;
		}

		int tuned = nextTuning(instrument.getTuning());
		storage.setTuning(tuning.getId(), tuned);
		int gained = xp.add(hundredthsPerTick(tuning.getTuningXp()));
		progress.addXp(gained);
		skills.addXp(tuning, gained);
		if (tuned >= 100)
		{
			message("Your " + tuning.getName() + " is now in tune.");
			stop();
		}
	}

	static int nextTuning(int tuning)
	{
		return Math.min(100, tuning + TUNING_PER_TICK);
	}

	/** The experience for one tick of tuning, in hundredths. */
	static int hundredthsPerTick(double tuningXp)
	{
		return FractionalXp.toHundredths(tuningXp) * TUNING_PER_TICK;
	}

	static boolean hasLevelFor(InstrumentDef def, int bardingLevel)
	{
		return bardingLevel >= def.getLevelRequired();
	}

	private void message(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}
}
