package com.barding.progress;

import com.barding.data.CraftRecipe;
import com.barding.data.InstrumentDef;
import com.barding.data.Material;
import com.barding.data.RiffText;
import com.barding.data.SkillRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

/** Works out what a player still needs to craft an instrument. Pure Java, so it can be tested. */
public final class CraftCheck
{
	private CraftCheck()
	{
	}

	/**
	 * Everything missing, in words, or an empty list if the instrument can be crafted.
	 * Materials are only checked: crafting doesn't consume them, as a plugin can't remove real items.
	 *
	 * @param skillLevel the player's level in a skill, by upper case name
	 * @param inventory how many of each item the player has, by lower case name
	 */
	public static List<String> missing(InstrumentDef def, int bardingLevel, ToIntFunction<String> skillLevel, Map<String, Integer> inventory)
	{
		List<String> missing = new ArrayList<>();
		if (bardingLevel < def.getLevelRequired())
		{
			missing.add(def.getLevelRequired() + " Barding");
		}
		CraftRecipe craft = def.getCraft();
		for (SkillRequirement requirement : craft.getRequirements())
		{
			if (skillLevel.applyAsInt(requirement.getSkill()) < requirement.getLevel())
			{
				missing.add(requirement.getLevel() + " " + RiffText.capitalize(requirement.getSkill()));
			}
		}
		for (Material material : craft.getMaterials())
		{
			if (carried(material, inventory) < material.getQuantity())
			{
				missing.add(material.getQuantity() + "x " + material.getItem());
			}
		}
		return missing;
	}

	private static int carried(Material material, Map<String, Integer> inventory)
	{
		int total = 0;
		for (String item : material.getAcceptedItems())
		{
			total += inventory.getOrDefault(item.toLowerCase(Locale.ROOT), 0);
		}
		return total;
	}
}
