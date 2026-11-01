package com.barding.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The specific kind of instrument. A standard instrument and its legendary version share a type,
 * and the family (the "Type" column in the Barding design doc) follows from it.
 */
@Getter
@RequiredArgsConstructor
public enum InstrumentType
{
	RECORDER(InstrumentFamily.WIND),
	BELLS(InstrumentFamily.PERCUSSION),
	REBEC(InstrumentFamily.STRING),
	ORGAN(InstrumentFamily.SPECIAL),
	BUISINE(InstrumentFamily.WIND),
	FRAME_DRUM(InstrumentFamily.PERCUSSION),
	LUTE(InstrumentFamily.STRING),
	WINDCHIMES(InstrumentFamily.SPECIAL),
	SACKBUT(InstrumentFamily.WIND),
	BONGOS(InstrumentFamily.PERCUSSION),
	DARBUKA(InstrumentFamily.PERCUSSION),
	SNAKE_CHARM(InstrumentFamily.WIND),
	SHELL_COLLECTION(InstrumentFamily.PERCUSSION),
	LYRE(InstrumentFamily.STRING),
	TABOR(InstrumentFamily.PERCUSSION),
	WOOD_FLUTE(InstrumentFamily.WIND),
	BANJO(InstrumentFamily.STRING),
	SHAWM(InstrumentFamily.WIND),
	ACCORDIAN(InstrumentFamily.SPECIAL),
	GUIRO(InstrumentFamily.PERCUSSION),
	GEMSHORN(InstrumentFamily.WIND),
	FIDDLE(InstrumentFamily.STRING),
	BODHRAN(InstrumentFamily.PERCUSSION),
	SYMBOLS(InstrumentFamily.PERCUSSION),
	HURDYGURDY(InstrumentFamily.STRING),
	CRYSTAL_FLUTE(InstrumentFamily.WIND),
	TEPONAZTI(InstrumentFamily.PERCUSSION),
	VIOL(InstrumentFamily.STRING),
	DIDGERIDOO(InstrumentFamily.WIND),
	STEEL_PAN(InstrumentFamily.PERCUSSION),
	SITAR(InstrumentFamily.STRING),
	FIFE(InstrumentFamily.WIND),
	WAR_DRUM(InstrumentFamily.PERCUSSION),
	OCARINA(InstrumentFamily.WIND),
	HARP(InstrumentFamily.STRING),
	SERPENT(InstrumentFamily.WIND),
	QUAD_DRUMS(InstrumentFamily.PERCUSSION),
	CITHARA(InstrumentFamily.STRING);

	private final InstrumentFamily family;
}
