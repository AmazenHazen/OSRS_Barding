package com.barding.data;

import java.util.List;
import lombok.Value;

/** One entry in the guide's left-hand list. A page with more than one tab shows a tab strip. */
@Value
public class GuidePage
{
	String title;
	List<GuideTab> tabs;
}
