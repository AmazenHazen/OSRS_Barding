package com.barding.progress;

import com.barding.data.InstrumentDef;
import com.barding.data.PubDef;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;

/**
 * Playing a carried instrument in a pub. It plays non-stop until the player moves away, giving
 * experience for each note hit, and each note may knock the instrument a little out of tune.
 * The animation and text above the player's head are only visible on this client. Client thread only.
 */
@Singleton
public class PlayService
{
	private static final int TICKS_PER_NOTE = 2;
	private static final int ANIMATION = AnimationID.VIKING_LYRE_PLAYING_LOOP;
	private static final int NO_ANIMATION = -1;
	private static final int OVERHEAD_CYCLES = 60;

	/** The share of a hit's Barding experience that also goes to the instrument. A placeholder until balanced. */
	static final double INSTRUMENT_XP_SHARE = 0.75;

	/** Chance out of 100 that a note knocks the instrument out of tune. A placeholder until balanced. */
	private static final double DRIFT_CHANCE = 5;

	/** Legendary instruments lose their tuning 20% slower. */
	private static final double LEGENDARY_DRIFT_MULTIPLIER = 0.8;

	private final Client client;
	private final InstrumentStorage storage;
	private final ProgressService progress;
	private final InstrumentSkills skills;
	private final FractionalXp bardingXp = new FractionalXp();
	private final FractionalXp instrumentXp = new FractionalXp();

	private List<PubDef> pubs = new ArrayList<>();
	private InstrumentDef playing;
	private WorldPoint startedAt;
	private int ticks;

	@Inject
	public PlayService(Client client, InstrumentStorage storage, ProgressService progress, InstrumentSkills skills)
	{
		this.client = client;
		this.storage = storage;
		this.progress = progress;
		this.skills = skills;
	}

	public void setPubs(List<PubDef> pubs)
	{
		this.pubs = pubs;
	}

	public boolean isPlaying(InstrumentDef def)
	{
		return playing != null && playing.getId().equals(def.getId());
	}

	public void start(InstrumentDef def)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		if (progress.getLevel() < def.getLevelRequired())
		{
			message("You need a Barding level of " + def.getLevelRequired() + " to play that.");
			return;
		}
		WorldPoint location = player.getWorldLocation();
		if (pubAt(location) == null)
		{
			message("You can only play in a pub.");
			return;
		}
		playing = def;
		startedAt = location;
		ticks = 0;
		message("You begin playing your " + def.getName() + ".");
		animate(player, ANIMATION);
	}

	/** Stops playing and shows the player's usual pose again. */
	public void stop()
	{
		if (playing == null)
		{
			return;
		}
		playing = null;
		startedAt = null;
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			animate(player, NO_ANIMATION);
		}
	}

	/** Call every game tick. */
	public void tick()
	{
		if (playing == null)
		{
			return;
		}
		InstrumentInstance instrument = storage.find(playing.getId()).orElse(null);
		Player player = client.getLocalPlayer();
		if (instrument == null || !instrument.isCarried() || player == null)
		{
			stop();
			return;
		}
		if (!player.getWorldLocation().equals(startedAt))
		{
			message("You stop playing your " + playing.getName() + ".");
			stop();
			return;
		}
		if (++ticks % TICKS_PER_NOTE == 0)
		{
			playNote(player, instrument);
		}
	}

	private void playNote(Player player, InstrumentInstance instrument)
	{
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double chance = NoteHit.chance(progress.getLevel(), skills.getRank(playing.getType()), instrument.getTuning(), NoteHit.STATIC_MASTERY);
		boolean hit = NoteHit.hits(chance, random.nextDouble(100));
		if (hit)
		{
			progress.addXp(bardingXp.add(FractionalXp.toHundredths(playing.getPlayXp())));
			skills.addXp(playing, instrumentXp.add(FractionalXp.toHundredths(playing.getPlayXp() * INSTRUMENT_XP_SHARE)));
		}
		if (drifts(playing.isLegendary(), random.nextDouble(100)))
		{
			storage.setTuning(playing.getId(), instrument.getTuning() - 1);
		}
		animate(player, ANIMATION);
		player.setOverheadText(hit ? "<col=00ff00>Note!</col>" : "<col=ff0000>Missed</col>");
		player.setOverheadCycle(OVERHEAD_CYCLES);
	}

	/** The pub at this location, or null if it isn't in one. */
	public PubDef pubAt(WorldPoint location)
	{
		for (PubDef pub : pubs)
		{
			if (pub.contains(location.getX(), location.getY(), location.getPlane()))
			{
				return pub;
			}
		}
		return null;
	}

	/** True if a note knocks the instrument out of tune, for a roll from 0 up to (but not including) 100. */
	static boolean drifts(boolean legendary, double roll)
	{
		return roll < DRIFT_CHANCE * (legendary ? LEGENDARY_DRIFT_MULTIPLIER : 1);
	}

	private static void animate(Player player, int animation)
	{
		player.setAnimation(animation);
		player.setAnimationFrame(0);
	}

	private void message(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}
}
