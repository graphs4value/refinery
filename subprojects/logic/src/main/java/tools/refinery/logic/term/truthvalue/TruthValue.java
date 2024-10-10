/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;

//Igazságérték
public enum TruthValue implements AbstractValue<TruthValue, Boolean> {
	TRUE("true"),

	FALSE("false"),

	UNKNOWN("unknown"),

	ERROR("error");

	private final String name;

	TruthValue(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static TruthValue toTruthValue(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	@Nullable
	public Boolean getArbitrary() {
		return switch (this) {
			case TRUE -> true;
			case FALSE, UNKNOWN -> false;
			case ERROR -> null;
		};
	}

	@Override
	public boolean isError() {
		return this == ERROR;
	}

	public boolean isConsistent() {
		return !isError();
	}

	public boolean isComplete() {
		return this != UNKNOWN;
	}

	//Ha igaz vagy hamis a truth value akkor ezeknek megfelelő booleannal tér vissza, egyébként meg nullal.
	@Override
	@Nullable
	public Boolean getConcrete() {
		return switch (this) {
			case TRUE -> true;
			case FALSE -> false;
			default -> null;
		};
	}

	@Override
	public boolean isConcrete() {
		return this == TRUE || this == FALSE;
	}

	public boolean must() {
		return this == TRUE || this == ERROR;
	}

	public boolean may() {
		return this == TRUE || this == UNKNOWN;
	}

	public TruthValue not() {
		return switch (this) {
			case TRUE -> FALSE;
			case FALSE -> TRUE;
			default -> this;
		};
	}

	@Override
	public TruthValue join(TruthValue other) {
		return switch (this) {
			case TRUE -> other == ERROR || other == TRUE ? TRUE : UNKNOWN;
			case FALSE -> other == ERROR || other == FALSE ? FALSE : UNKNOWN;
			case UNKNOWN -> UNKNOWN;
			case ERROR -> other;
		};
	}

	//Két TruthValue értékein alapul
	@Override
	public TruthValue meet(TruthValue other) {
		return switch (this) {
			//Ha az egyik igaz és a másik igaz vagy unknown akkor igaz, egyébként error.
			case TRUE -> other == UNKNOWN || other == TRUE ? TRUE : ERROR;
			//Ha az egyik hamis és a másik hamis vagy unknown akkor hamis, egyébként error.
			case FALSE -> other == UNKNOWN || other == FALSE ? FALSE : ERROR;
			//Ha az egyik unknown akkor a másik.
			case UNKNOWN -> other;
			//Ha az egyik error akkor az.
			case ERROR -> ERROR;
		};
	}
}
