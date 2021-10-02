package org.eclipse.viatra.solver.data.model.representation;

public enum TruthValue {
	TRUE("true"),

	FALSE("false"),

	UNKNOWN("unknown"),

	ERROR("error");

	private final String name;

	private TruthValue(String name) {
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
		if (this == TRUE) {
			return FALSE;
		} else if (this == FALSE) {
			return TRUE;
		} else {
			return this;
		}
	}
}
