package com.barding.data;

import java.util.List;
import lombok.Value;

/** One piece of guide content. type is "heading", "text", "bullets" or "instruments". */
@Value
public class GuideBlock
{
	String type;

	/** Used by "heading" and "text". */
	String text;

	/** Used by "bullets". */
	List<String> items;
}
