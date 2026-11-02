package com.barding.data;

import lombok.Value;

/** A pub the player can play in, as described in pubs.json. The corners are world coordinates, inclusive. */
@Value
public class PubDef
{
	/** Stable snake_case key. */
	String id;
	String name;
	int minX;
	int minY;
	int maxX;
	int maxY;
	int plane;

	public boolean contains(int x, int y, int atPlane)
	{
		return atPlane == plane && x >= minX && x <= maxX && y >= minY && y <= maxY;
	}
}
