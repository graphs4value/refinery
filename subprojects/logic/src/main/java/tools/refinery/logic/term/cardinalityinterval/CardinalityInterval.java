/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.cardinalityinterval;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;

public record CardinalityInterval(int lowerBound, UpperCardinality upperBound)
		implements AbstractValue<CardinalityInterval, Integer> {
	public CardinalityInterval {
		if (lowerBound < 0) {
			throw new IllegalArgumentException("lowerBound must not be negative");
		}
	}

	@Nullable
	@Override
	public Integer getConcrete() {
		return isConcrete() ? lowerBound : null;
	}

	@Override
	public boolean isConcrete() {
		return upperBound.compareToInt(lowerBound) == 0;
	}

	@Nullable
	@Override
	public Integer getArbitrary() {
		return isError() ? null : lowerBound;
	}

	@Override
	public boolean isError() {
		return upperBound.compareToInt(lowerBound) < 0;
	}

	@Override
	public boolean isRefinementOf(CardinalityInterval other) {
		return lowerBound >= other.lowerBound && upperBound.compareTo(other.upperBound) <= 0;
	}

	public CardinalityInterval min(CardinalityInterval other) {
		return new CardinalityInterval(Math.min(lowerBound, other.lowerBound), upperBound.min(other.upperBound));
	}

	public CardinalityInterval max(CardinalityInterval other) {
		return new CardinalityInterval(Math.max(lowerBound, other.lowerBound), upperBound.max(other.upperBound));
	}

	public CardinalityInterval add(CardinalityInterval other) {
		return new CardinalityInterval(lowerBound + other.lowerBound, upperBound.add(other.upperBound));
	}

	public CardinalityInterval multiply(CardinalityInterval other) {
		return new CardinalityInterval(lowerBound * other.lowerBound, upperBound.multiply(other.upperBound));
	}

	@Override
	public CardinalityInterval meet(CardinalityInterval other) {
		return new CardinalityInterval(Math.max(lowerBound, other.lowerBound), upperBound.min(other.upperBound));
	}

	@Override
	public CardinalityInterval join(CardinalityInterval other) {
		return new CardinalityInterval(Math.min(lowerBound, other.lowerBound), upperBound.max(other.upperBound));
	}

	@Nullable
	public CardinalityInterval take(int count) {
		int newLowerBound = Math.max(lowerBound - count, 0);
		var newUpperBound = upperBound.take(count);
		if (newUpperBound == null) {
			return null;
		}
		return new CardinalityInterval(newLowerBound, newUpperBound);
	}

	@Override
	public String toString() {
		if (upperBound instanceof FiniteUpperCardinality(var finiteUpperBound) && finiteUpperBound == lowerBound) {
			return "[%d]".formatted(lowerBound);
		}
		return "[%d..%s]".formatted(lowerBound, upperBound);
	}
}
