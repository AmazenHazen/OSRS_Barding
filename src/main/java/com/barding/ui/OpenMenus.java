package com.barding.ui;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetModalMode;

/** Answers questions about the game's own menus, so our guide window can behave like one of them. */
@Singleton
public class OpenMenus
{
	private final Client client;

	@Inject
	public OpenMenus(Client client)
	{
		this.client = client;
	}

	/**
	 * Adds a right-click option above everything else, which also makes it the left-click action.
	 * It only runs code in the plugin: nothing is sent to the server.
	 */
	public void addOption(String option, String target, Runnable action)
	{
		client.getMenu().createMenuEntry(-1)
			.setOption(option)
			.setTarget("<col=ff9040>" + target + "</col>")
			.setType(MenuAction.RUNELITE)
			.onClick(entry -> action.run());
	}

	/** True if the interface group is open as a menu, as opposed to a side tab or an on-screen overlay. */
	public boolean isModal(int groupId)
	{
		for (WidgetNode node : client.getComponentTable())
		{
			if (node.getId() == groupId && node.getModalMode() != WidgetModalMode.NON_MODAL)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Closes any open vanilla skill guide. This only closes it on the client: telling the server would
	 * mean sending an action, which plugins may not do. A skill guide holds no server-side state.
	 */
	public void closeSkillGuides()
	{
		WidgetNode open = null;
		for (WidgetNode node : client.getComponentTable())
		{
			if (node.getId() == InterfaceID.SKILL_GUIDE || node.getId() == InterfaceID.SKILL_GUIDE_V2)
			{
				open = node;
				break;
			}
		}
		if (open != null)
		{
			client.closeInterface(open, true);
		}
	}
}
