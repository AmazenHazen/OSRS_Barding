package com.barding.data;

import lombok.Value;

/** A level the player needs in some skill (e.g. 15 Smithing). */
@Value
public class SkillRequirement
{
	/** Upper case skill name, e.g. "SMITHING". */
	String skill;
	int level;
}
