package com.barding.data;

import java.util.List;
import lombok.Value;

/**
 * A quantity of one item used in a crafting recipe. "item" is the real item's name, unless
 * "anyOf" is present: then "item" is just a label, and any mix of the listed items counts.
 */
@Value
public class Material
{
	String item;
	int quantity;
	List<String> anyOf;

	/** The real item names that count towards this material. */
	public List<String> getAcceptedItems()
	{
		return anyOf == null ? List.of(item) : anyOf;
	}
}
