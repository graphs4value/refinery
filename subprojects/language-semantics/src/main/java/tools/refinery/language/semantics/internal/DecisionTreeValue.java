/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import tools.refinery.store.representation.TruthValue;

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

	public TruthValue merge(TruthValue other) {
		return truthValue == null ? other : truthValue.merge(other);
	}

	public DecisionTreeValue overwrite(DecisionTreeValue other) {
		return other == UNSET ? this : other;
	}

	public TruthValue getTruthValueOrElse(TruthValue other) {
		return this == UNSET ? other : truthValue;
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
