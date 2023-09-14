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

public class ConcreteSymbolRefiner<A, C> extends AbstractPartialInterpretationRefiner<A, C> {
	private final Interpretation<A> interpretation;

	public ConcreteSymbolRefiner(ReasoningAdapter adapter, PartialSymbol<A, C> partialSymbol,
								 Symbol<A> concreteSymbol) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
	}

	@Override
	public boolean merge(Tuple key, A value) {
		var currentValue = interpretation.get(key);
		var mergedValue = getPartialSymbol().abstractDomain().commonRefinement(currentValue, value);
		if (!Objects.equals(currentValue, mergedValue)) {
			interpretation.put(key, mergedValue);
		}
		return true;
	}

	public static <A1, C1> Factory<A1, C1> of(Symbol<A1> concreteSymbol) {
		return (adapter, partialSymbol) -> new ConcreteSymbolRefiner<>(adapter, partialSymbol, concreteSymbol);
	}
}
