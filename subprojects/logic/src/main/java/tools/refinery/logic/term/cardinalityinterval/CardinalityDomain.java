/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.cardinalityinterval;

import tools.refinery.logic.AbstractDomain;

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
	public CardinalityInterval unknown() {
		return CardinalityIntervals.SET;
	}

	@Override
	public CardinalityInterval error() {
		return CardinalityIntervals.ERROR;
	}

	@Override
	public CardinalityInterval toAbstract(Integer concreteValue) {
		return CardinalityIntervals.exactly(concreteValue);
	}
}
