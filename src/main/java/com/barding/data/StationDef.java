package com.barding.data;

import lombok.Value;

/** Where a musician's bench goes, as described in stations.json. x, y and plane are world coordinates. */
@Value
public class StationDef
{
	/** Stable snake_case key. Saved placements refer to it, so never change it once released. */
	String id;
	String name;
	int x;
	int y;
	int plane;

	/** 0 to 2047, where 512 is a quarter turn. */
	int orientation;
}
