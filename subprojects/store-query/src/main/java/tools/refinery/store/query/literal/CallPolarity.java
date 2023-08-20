/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.InvalidQueryException;

public enum CallPolarity {
	POSITIVE(true, false),
	NEGATIVE(false, false),
	TRANSITIVE(true, true);

	private final boolean positive;

	private final boolean transitive;

	CallPolarity(boolean positive, boolean transitive) {
		this.positive = positive;
		this.transitive = transitive;
	}

	public boolean isPositive() {
		return positive;
	}

	public boolean isTransitive() {
		return transitive;
	}

	public CallPolarity negate() {
		return switch (this) {
			case POSITIVE -> NEGATIVE;
			case NEGATIVE -> POSITIVE;
			case TRANSITIVE -> throw new InvalidQueryException("Transitive polarity cannot be negated");
		};
	}
}
