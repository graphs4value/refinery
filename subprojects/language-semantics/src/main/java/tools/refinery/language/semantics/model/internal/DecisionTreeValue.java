package tools.refinery.language.semantics.model.internal;

import tools.refinery.store.model.representation.TruthValue;

public enum DecisionTreeValue {
	UNSET(null),
	TRUE(TruthValue.TRUE),
	FALSE(TruthValue.FALSE),
	UNKNOWN(TruthValue.UNKNOWN),
	ERROR(TruthValue.ERROR);

	final TruthValue truthValue;

	DecisionTreeValue(TruthValue truthValue) {
		this.truthValue = truthValue;
	}

	public TruthValue getTruthValue() {
		return truthValue;
	}

	public DecisionTreeValue merge(TruthValue other) {
		return truthValue == null ? fromTruthValue(other) : fromTruthValue(truthValue.merge(other));
	}

	public DecisionTreeValue overwrite(DecisionTreeValue other) {
		return other == UNSET ? this : other;
	}

	public static DecisionTreeValue fromTruthValue(TruthValue truthValue) {
		if (truthValue == null) {
			return DecisionTreeValue.UNSET;
		}
		return switch (truthValue) {
			case TRUE -> TRUE;
			case FALSE -> FALSE;
			case UNKNOWN -> UNKNOWN;
			case ERROR -> ERROR;
		};
	}
}
