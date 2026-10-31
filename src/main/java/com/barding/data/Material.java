package com.barding.data;

import lombok.Value;

/** A quantity of one item used in a crafting recipe. */
@Value
public class Material
{
	String item;
	int quantity;
}
