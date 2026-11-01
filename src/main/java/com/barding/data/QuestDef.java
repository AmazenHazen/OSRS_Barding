package com.barding.data;

import java.util.List;
import lombok.Value;

/**
 * One Barding quest, as described in quests.json. This is pure data:
 * no RuneLite types, so it can be loaded and tested without a running client.
 *
 * "location" may be absent (null). Requirement lists are empty, not absent, when there are none.
 */
@Value
public class QuestDef
{
	/** Stable snake_case key used for references and saved progress. Never change it once released. */
	String id;

	/** Display name shown in the skill guide. */
	String name;
	QuestDifficulty difficulty;
	String location;
	String description;

	List<SkillRequirement> skillRequirements;

	/** Names of existing game quests that must be completed first. */
	List<String> questRequirements;

	List<String> rewards;
}
