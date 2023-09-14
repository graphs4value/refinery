/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import tools.refinery.store.representation.AbstractDomain;

import java.util.Optional;

// Singleton pattern, because there is only one domain for truth values.
@SuppressWarnings("squid:S6548")
public class CardinalityDomain implements AbstractDomain<CardinalityInterval, Integer> {
	public static final CardinalityDomain INSTANCE = new CardinalityDomain();

	private CardinalityDomain() {
	}

	@Override
	public Class<CardinalityInterval> abstractType() {
		return CardinalityInterval.class;
	}

	@Override
	public Class<Integer> concreteType() {
		return Integer.class;
	}

	@Override
	public CardinalityInterval toAbstract(Integer concreteValue) {
		return CardinalityIntervals.exactly(concreteValue);
	}

	@Override
	public Optional<Integer> toConcrete(CardinalityInterval abstractValue) {
		return isConcrete(abstractValue) ? Optional.of(abstractValue.lowerBound()) : Optional.empty();
	}

	@Override
	public boolean isConcrete(CardinalityInterval abstractValue) {
		if (!(abstractValue instanceof NonEmptyCardinalityInterval nonEmptyValue) ||
				!((nonEmptyValue.upperBound()) instanceof FiniteUpperCardinality finiteUpperCardinality)) {
			return false;
		}
		return nonEmptyValue.lowerBound() == finiteUpperCardinality.finiteUpperBound();
	}

	@Override
	public CardinalityInterval commonRefinement(CardinalityInterval leftValue, CardinalityInterval rightValue) {
		return leftValue.meet(rightValue);
	}

	@Override
	public CardinalityInterval commonAncestor(CardinalityInterval leftValue, CardinalityInterval rightValue) {
		return leftValue.join(rightValue);
	}

	@Override
	public CardinalityInterval unknown() {
		return CardinalityIntervals.SET;
	}

	@Override
	public boolean isError(CardinalityInterval abstractValue) {
		return abstractValue.isEmpty();
	}
}
