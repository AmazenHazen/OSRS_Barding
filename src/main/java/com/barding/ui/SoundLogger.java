package com.barding.ui;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MidiRequest;
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.eventbus.Subscribe;

/** A debugging aid: prints every sound the game plays, for finding sound ids. Only registered while switched on. */
@Singleton
public class SoundLogger
{
	private final Client client;
	private final Set<Integer> jinglesSeen = new HashSet<>();

	@Inject
	public SoundLogger(Client client)
	{
		this.client = client;
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		print("Sound effect " + event.getSoundId());
	}

	@Subscribe
	public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event)
	{
		print("Area sound " + event.getSoundId());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Set<Integer> active = new HashSet<>();
		for (MidiRequest request : client.getActiveMidiRequests())
		{
			if (request.isJingle())
			{
				active.add(request.getArchiveId());
				if (!jinglesSeen.contains(request.getArchiveId()))
				{
					print("Jingle " + request.getArchiveId());
				}
			}
		}
		jinglesSeen.clear();
		jinglesSeen.addAll(active);
	}

	private void print(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}
}
