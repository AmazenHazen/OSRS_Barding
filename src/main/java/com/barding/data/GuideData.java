package com.barding.data;

import java.util.List;
import lombok.Value;

/** Everything in guide.json. */
@Value
public class GuideData
{
	List<GuidePage> pages;
}
