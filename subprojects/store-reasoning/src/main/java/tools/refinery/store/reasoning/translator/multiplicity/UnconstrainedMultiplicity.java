/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiplicity;

import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;

// Singleton implementation, because there is only a single complete interval.
@SuppressWarnings("squid:S6548")
public final class UnconstrainedMultiplicity implements Multiplicity {
	public static final UnconstrainedMultiplicity INSTANCE = new UnconstrainedMultiplicity();

	private UnconstrainedMultiplicity() {
	}

	@Override
	public CardinalityInterval multiplicity() {
		return CardinalityIntervals.SET;
	}

	@Override
	public boolean isConstrained() {
		return true;
	}
}
