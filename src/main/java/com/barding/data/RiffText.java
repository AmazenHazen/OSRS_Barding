package com.barding.data;

import java.util.List;

/** Turns a Riff into a one-line description. Pure Java, so both the sidebar and the guide window can share it. */
public final class RiffText
{
	private RiffText()
	{
	}

	public static String describe(Riff riff)
	{
		String cooldown = riff.getCooldownMinutes() > 0 ? " (" + riff.getCooldownMinutes() + " min cooldown)" : "";
		switch (riff.getEffect())
		{
			case SKILL_BOOST:
			{
				String area = riff.getAreaSize() > 1 ? ", " + riff.getAreaSize() + "x" + riff.getAreaSize() + " area" : "";
				return "+" + riff.getBoost() + " " + joinSkills(riff.getSkills())
					+ " for " + (riff.getDurationSeconds() / 60) + " min" + area + cooldown;
			}
			case STAMINA:
				return "Stamina boost, " + riff.getDurationSeconds() + "s" + cooldown;
			case PRAYER_DRAIN_PAUSE:
				return "Pauses prayer drain, " + riff.getDurationSeconds() + "s" + cooldown;
			default:
				return riff.getDescription() + cooldown;
		}
	}

	private static String joinSkills(List<String> skills)
	{
		if (skills == null || skills.isEmpty())
		{
			return "?";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < skills.size(); i++)
		{
			if (i > 0)
			{
				sb.append(i == skills.size() - 1 ? " & " : ", ");
			}
			sb.append(capitalize(skills.get(i)));
		}
		return sb.toString();
	}

	public static String capitalize(String s)
	{
		if (s == null || s.isEmpty())
		{
			return "";
		}
		return s.charAt(0) + s.substring(1).toLowerCase();
	}
}
