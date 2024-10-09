/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class ConcreteRelationRefiner extends
		AbstractPartialInterpretationRefiner.ConcretizationAware<TruthValue, Boolean> {
	private final Interpretation<TruthValue> interpretation;
	private final RoundingMode roundingMode;

	protected ConcreteRelationRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
									  Symbol<TruthValue> concreteSymbol, RoundingMode roundingMode) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
		this.roundingMode = roundingMode;
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var currentValue = get(key);
		if (forbiddenByConcretization(currentValue, value)) {
			// Make sure to avoid
			return false;
		}
		var mergedValue = currentValue.meet(value);
		if (!Objects.equals(currentValue, mergedValue)) {
			put(key, mergedValue);
		}
		return true;
	}

	protected boolean forbiddenByConcretization(TruthValue oldValue, TruthValue newValue) {
		return shouldCheckConcretization(oldValue, newValue) && concretizationInProgress();
	}

	protected boolean shouldCheckConcretization(TruthValue oldValue, TruthValue newValue) {
		return switch (roundingMode) {
			case NONE -> false;
			case PREFER_FALSE -> !oldValue.must() && newValue == TruthValue.TRUE;
			case PREFER_TRUE -> oldValue.may() && newValue == TruthValue.FALSE;
		};
	}

	protected TruthValue get(Tuple key) {
		return interpretation.get(key);
	}

	protected TruthValue put(Tuple key, TruthValue value) {
		return interpretation.put(key, value);
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol, RoundingMode roundingMode) {
		if (roundingMode == RoundingMode.NONE) {
			return ConcreteSymbolRefiner.of(concreteSymbol);
		}
		return (adapter, partialSymbol) -> new ConcreteRelationRefiner(adapter, partialSymbol, concreteSymbol,
				roundingMode);
	}
}
