/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.AbstractPartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

public class EqualsRefiner extends AbstractPartialInterpretationRefiner<TruthValue, Boolean> {
	private final Interpretation<CardinalityInterval> countInterpretation;

	private EqualsRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
                          Symbol<CardinalityInterval> countSymbol) {
		super(adapter, partialSymbol);
		countInterpretation = adapter.getModel().getInterpretation(countSymbol);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		if (value == TruthValue.UNKNOWN) {
			return true;
		}
		if (value == TruthValue.ERROR) {
			return false;
		}
		int left = key.get(0);
		int right = key.get(1);
		boolean isDiagonal = left == right;
		if (isDiagonal && value == TruthValue.FALSE) {
			return false;
		}
		if (!isDiagonal) {
			return !value.may();
		}
		if (value != TruthValue.TRUE) {
			throw new IllegalArgumentException("Unknown TruthValue: " + value);
		}
		// {@code isDiagonal} is true, so this could be {@code left} or {@code right}.
		var unaryKey = Tuple.of(left);
		var currentCount = countInterpretation.get(unaryKey);
		if (currentCount == null) {
			return false;
		}
		var newCount = currentCount.meet(CardinalityIntervals.LONE);
		if (newCount.isEmpty()) {
			return false;
		}
		countInterpretation.put(unaryKey, newCount);
		return true;
	}

	public static Factory<TruthValue, Boolean> of(Symbol<CardinalityInterval> countSymbol) {
		return (adapter, partialSymbol) -> new EqualsRefiner(adapter, partialSymbol, countSymbol);
	}
}
