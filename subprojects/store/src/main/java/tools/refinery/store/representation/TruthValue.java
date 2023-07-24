/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation;

public enum TruthValue {
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

	public boolean isConsistent() {
		return this != ERROR;
	}

	public boolean isComplete() {
		return this != UNKNOWN;
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

	public TruthValue merge(TruthValue other) {
		return switch (this) {
			case TRUE -> other == UNKNOWN || other == TRUE ? TRUE : ERROR;
			case FALSE -> other == TruthValue.UNKNOWN || other == TruthValue.FALSE ? FALSE : ERROR;
			case UNKNOWN -> other;
			default -> ERROR;
		};
	}
}
