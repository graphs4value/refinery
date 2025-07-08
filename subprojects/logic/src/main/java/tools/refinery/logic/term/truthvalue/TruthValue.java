/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.ComparableAbstractValue;
import tools.refinery.logic.term.operators.And;
import tools.refinery.logic.term.operators.Not;
import tools.refinery.logic.term.operators.Or;
import tools.refinery.logic.term.operators.Xor;

public enum TruthValue implements ComparableAbstractValue<TruthValue, Boolean>, Not<TruthValue>, And<TruthValue>,
		Or<TruthValue>, Xor<TruthValue> {
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

	@Override
	public String toString() {
		return getName();
	}

	public static TruthValue of(boolean value) {
		return value ? TRUE : FALSE;
	}

	public static TruthValue of(boolean may, boolean must) {
		if (may) {
			return must ? TRUE : UNKNOWN;
		}
		return must ? ERROR : FALSE;
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

	@Override
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

	@Override
	public TruthValue meet(TruthValue other) {
		return switch (this) {
			case TRUE -> other == UNKNOWN || other == TRUE ? TRUE : ERROR;
			case FALSE -> other == UNKNOWN || other == FALSE ? FALSE : ERROR;
			case UNKNOWN -> other;
			case ERROR -> ERROR;
		};
	}

	@Override
	public TruthValue and(TruthValue other) {
		return switch (this) {
			case UNKNOWN -> other.may() ? TruthValue.UNKNOWN : TruthValue.FALSE;
			case FALSE -> TruthValue.FALSE;
			case TRUE -> other;
			case ERROR -> other.must() ? TruthValue.ERROR : TruthValue.FALSE;
		};
	}

	@Override
	public TruthValue or(TruthValue other) {
		return switch (this) {
			case UNKNOWN -> other.must() ? TruthValue.TRUE : TruthValue.UNKNOWN;
			case FALSE -> other;
			case TRUE -> TruthValue.TRUE;
			case ERROR -> other.may() ? TruthValue.TRUE : TruthValue.ERROR;
		};
	}

	@Override
	public TruthValue xor(TruthValue other) {
		return checkEquals(other).not();
	}

	@Override
	public TruthValue checkEquals(TruthValue other) {
		return switch (this) {
			case UNKNOWN -> other == ERROR ? ERROR : TruthValue.UNKNOWN;
			case TRUE -> other;
			case FALSE -> other.not();
			case ERROR -> ERROR;
		};
	}

	@Override
	public TruthValue checkLess(TruthValue other) {
		return switch (this) {
			case UNKNOWN -> other.may() ? UNKNOWN : FALSE;
			case TRUE -> FALSE;
			case FALSE -> other;
			case ERROR -> other.must() ? ERROR : FALSE;
		};
	}

	@Override
	public TruthValue checkLessEq(TruthValue other) {
		return switch(this) {
			case UNKNOWN -> other.must() ? TRUE : UNKNOWN;
			case TRUE -> other;
			case FALSE -> TRUE;
			case ERROR -> other.may() ? TRUE : ERROR;
		};
	}

	@Override
	public TruthValue upToIncluding(TruthValue other) {
		if (must()) {
			return other.may() ? TRUE : ERROR;
		}
		return other.may() ? UNKNOWN : FALSE;
	}

	@Override
	public TruthValue min(TruthValue other) {
		return and(other);
	}

	@Override
	public TruthValue max(TruthValue other) {
		return or(other);
	}
}
