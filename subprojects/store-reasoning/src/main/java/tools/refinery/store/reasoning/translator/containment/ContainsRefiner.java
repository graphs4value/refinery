/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.AbstractPartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

class ContainsRefiner extends AbstractPartialInterpretationRefiner<TruthValue, Boolean> {
	private static final InferredContainment CONTAINS_TRUE = new InferredContainment(TruthValue.TRUE, Set.of(),
			Set.of());
	private static final InferredContainment CONTAINS_FALSE = new InferredContainment(TruthValue.FALSE, Set.of(),
			Set.of());
	private static final InferredContainment CONTAINS_ERROR = new InferredContainment(TruthValue.ERROR, Set.of(),
			Set.of());

	private final PartialInterpretationRefiner<TruthValue, Boolean> containedRefiner;
	private final Interpretation<InferredContainment> interpretation;

	public ContainsRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
						   Symbol<InferredContainment> symbol) {
		super(adapter, partialSymbol);
		containedRefiner = adapter.getRefiner(ContainmentHierarchyTranslator.CONTAINED_SYMBOL);
		interpretation = adapter.getModel().getInterpretation(symbol);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var oldValue = interpretation.get(key);
		var newValue = mergeContains(oldValue, value);
		if (oldValue != newValue) {
			interpretation.put(key, newValue);
		}
		if (value.must()) {
			return containedRefiner.merge(Tuple.of(key.get(1)), TruthValue.TRUE);
		}
		return true;
	}

	public InferredContainment mergeContains(InferredContainment oldValue, TruthValue toMerge) {
		var oldContains = oldValue.contains();
		var newContains = oldContains.merge(toMerge);
		if (newContains == oldContains) {
			return oldValue;
		}
		if (oldValue.mustLinks().isEmpty() && oldValue.forbiddenLinks().isEmpty()) {
			return switch (toMerge) {
				case UNKNOWN -> oldValue;
				case TRUE -> oldContains.may() ? CONTAINS_TRUE : CONTAINS_ERROR;
				case FALSE -> oldContains.must() ? CONTAINS_ERROR : CONTAINS_FALSE;
				case ERROR -> CONTAINS_ERROR;
			};
		}
		return new InferredContainment(newContains, oldValue.mustLinks(), oldValue.forbiddenLinks());
	}

	public static Factory<TruthValue, Boolean> of(Symbol<InferredContainment> symbol) {
		return (adapter, partialSymbol) -> new ContainsRefiner(adapter, partialSymbol, symbol);
	}
}
