/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.literal;

import tools.refinery.logic.InvalidQueryException;

/**
 * Represents the polarity of a call. A call can be positive, negative, or transitive.

 */
public enum CallPolarity {
	POSITIVE(true, false),
	NEGATIVE(false, false),
	TRANSITIVE(true, true);

	//In terms of positivity only negative is false.
	private final boolean positive;

	//Only transitive calls are transitive.
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

	/**
	 * Negates the polarity of the call. A transitive call cannot be negated.
	 *
	 * @return the negated polarity
	 */
	public CallPolarity negate() {
		return switch (this) {
			case POSITIVE -> NEGATIVE;
			case NEGATIVE -> POSITIVE;
			case TRANSITIVE -> throw new InvalidQueryException("Transitive polarity cannot be negated");
		};
	}
}
