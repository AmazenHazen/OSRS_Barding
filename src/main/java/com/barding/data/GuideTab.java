package com.barding.data;

import java.util.List;
import lombok.Value;

/** A sub-tab along the top of a guide page (e.g. "Overview" / "Quests"). */
@Value
public class GuideTab
{
	String title;
	List<GuideBlock> blocks;
}
