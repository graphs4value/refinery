/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.cardinalityinterval;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;

// Singleton implementation, because there is only a single empty interval.
@SuppressWarnings("squid:S6548")
public final class EmptyCardinalityInterval implements CardinalityInterval {
	static final EmptyCardinalityInterval INSTANCE = new EmptyCardinalityInterval();

	private EmptyCardinalityInterval() {
		// Singleton constructor.
	}

	@Override
	@Nullable
	public Integer getConcrete() {
		return null;
	}

	@Override
	@Nullable
	public Integer getArbitrary() {
		return null;
	}

	@Override
	public boolean isRefinementOf(CardinalityInterval other) {
		return true;
	}

	@Override
	public int lowerBound() {
		return 1;
	}

	@Override
	public UpperCardinality upperBound() {
		return UpperCardinalities.ZERO;
	}

	@Override
	public CardinalityInterval min(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval max(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval add(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval take(int count) {
		return this;
	}

	@Override
	public CardinalityInterval multiply(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval meet(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval join(CardinalityInterval other) {
		return other;
	}

	@Override
	public String toString() {
		return "error";
	}
}
