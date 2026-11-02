package com.barding.progress;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The four tuning levels from the design doc. The percentages they start at are placeholders. */
@Getter
@RequiredArgsConstructor
public enum TuningState
{
	IN_TUNE("In tune", 100),
	SLIGHTLY_OUT("Slightly out of tune", 60),
	OUT("Out of tune", 30),
	EXTREMELY_OUT("Extremely out of tune", 0);

	private final String label;
	private final int minTuning;

	public static TuningState of(int tuning)
	{
		for (TuningState state : values())
		{
			if (tuning >= state.minTuning)
			{
				return state;
			}
		}
		return EXTREMELY_OUT;
	}
}
