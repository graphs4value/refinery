/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class ConcreteSymbolRefiner<A, C> implements PartialInterpretationRefiner<A, C> {
	private final ReasoningAdapter adapter;
	private final PartialSymbol<A, C> partialSymbol;
	private final Interpretation<A> interpretation;

	public ConcreteSymbolRefiner(ReasoningAdapter adapter, PartialSymbol<A, C> partialSymbol,
								 Symbol<A> concreteSymbol) {
		this.adapter = adapter;
		this.partialSymbol = partialSymbol;
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
	}

	@Override
	public ReasoningAdapter getAdapter() {
		return adapter;
	}

	@Override
	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}

	@Override
	public boolean merge(Tuple key, A value) {
		var currentValue = interpretation.get(key);
		var mergedValue = partialSymbol.abstractDomain().commonRefinement(currentValue, value);
		if (!Objects.equals(currentValue, mergedValue)) {
			interpretation.put(key, mergedValue);
		}
		return true;
	}

	public static <A1, C1> PartialInterpretationRefiner.Factory<A1, C1> of(Symbol<A1> concreteSymbol) {
		return (adapter, partialSymbol) -> new ConcreteSymbolRefiner<>(adapter, partialSymbol, concreteSymbol);
	}
}
