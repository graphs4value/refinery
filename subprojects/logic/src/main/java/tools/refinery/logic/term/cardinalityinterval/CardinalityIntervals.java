/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.cardinalityinterval;

import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;

public final class CardinalityIntervals {
	public static final CardinalityInterval NONE = exactly(0);

	public static final CardinalityInterval ONE = exactly(1);

	public static final CardinalityInterval LONE = atMost(1);

	public static final CardinalityInterval SET = atLeast(0);

	public static final CardinalityInterval SOME = atLeast(1);

	public static final CardinalityInterval ERROR = between(1, UpperCardinalities.ZERO);

	private CardinalityIntervals() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static CardinalityInterval between(int lowerBound, UpperCardinality upperBound) {
		return new CardinalityInterval(lowerBound, upperBound);
	}

	public static CardinalityInterval between(int lowerBound, int upperBound) {
		return between(lowerBound, UpperCardinalities.atMost(upperBound));
	}

	public static CardinalityInterval atMost(UpperCardinality upperBound) {
		return new CardinalityInterval(0, upperBound);
	}

	public static CardinalityInterval atMost(int upperBound) {
		return atMost(UpperCardinalities.atMost(upperBound));
	}

	public static CardinalityInterval atLeast(int lowerBound) {
		return new CardinalityInterval(lowerBound, UpperCardinalities.UNBOUNDED);
	}

	public static CardinalityInterval exactly(int lowerBound) {
		return new CardinalityInterval(lowerBound, UpperCardinalities.atMost(lowerBound));
	}
}
