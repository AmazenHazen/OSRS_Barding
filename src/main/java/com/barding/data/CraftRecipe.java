package com.barding.data;

import java.util.List;
import lombok.Value;

/** What it takes to craft an instrument at a Musician's Workshop. */
@Value
public class CraftRecipe
{
	List<SkillRequirement> requirements;
	List<Material> materials;
}
