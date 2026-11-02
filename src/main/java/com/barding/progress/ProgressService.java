package com.barding.progress;

import com.barding.BardingConfig;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;

/**
 * The player's Barding experience, saved per account in their RuneLite profile.
 * Everything that awards experience goes through addXp().
 */
@Slf4j
@Singleton
public class ProgressService
{
	private static final String XP_KEY = "xp";

	private final Client client;
	private final ConfigManager configManager;
	private final XpDrops drops;

	private volatile BardingXp xp = BardingXp.NONE;
	private IntConsumer onLevelUp = level -> { };

	@Inject
	public ProgressService(Client client, ConfigManager configManager, XpDrops drops)
	{
		this.client = client;
		this.configManager = configManager;
		this.drops = drops;
	}

	/** Runs with the new level whenever the player levels up. */
	public void setOnLevelUp(IntConsumer onLevelUp)
	{
		this.onLevelUp = onLevelUp;
	}

	public BardingXp getXp()
	{
		return xp;
	}

	public int getLevel()
	{
		return xp.getLevel();
	}

	/** Reads the current account's saved progress. Call on startup and whenever the account changes. */
	public void load()
	{
		xp = BardingXp.parse(configManager.getRSProfileConfiguration(BardingConfig.GROUP, XP_KEY));
		log.debug("Loaded {} Barding xp", xp.getXp());
	}

	/** Adds experience and saves it, announcing a level up like the game does. Client thread only. */
	public void addXp(int amount)
	{
		if (amount <= 0)
		{
			return;
		}
		int oldLevel = getLevel();
		setXp(xp.plus(amount));
		drops.add(null, amount);
		int newLevel = getLevel();
		if (newLevel > oldLevel)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"Congratulations, you've just advanced your Barding level. You are now level " + newLevel + ".", null);
			onLevelUp.accept(newLevel);
		}
	}

	/** Back to zero. Only for testing. */
	public void reset()
	{
		setXp(BardingXp.NONE);
	}

	private void setXp(BardingXp value)
	{
		if (configManager.getRSProfileKey() == null)
		{
			log.debug("Ignoring Barding xp change: no account is logged in");
			return;
		}
		xp = value;
		configManager.setRSProfileConfiguration(BardingConfig.GROUP, XP_KEY, value.getXp());
	}
}
