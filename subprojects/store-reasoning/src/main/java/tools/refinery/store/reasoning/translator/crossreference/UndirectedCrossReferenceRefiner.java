/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteSymbolRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

class UndirectedCrossReferenceRefiner extends ConcreteSymbolRefiner<TruthValue, Boolean> {
	private final PartialInterpretationRefiner<TruthValue, Boolean> sourceRefiner;

	public UndirectedCrossReferenceRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
										   Symbol<TruthValue> concreteSymbol, PartialRelation sourceType) {
		super(adapter, partialSymbol, concreteSymbol);
		sourceRefiner = adapter.getRefiner(sourceType);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		int source = key.get(0);
		int target = key.get(1);
		if (!super.merge(key, value) || !super.merge(Tuple.of(target, source), value)) {
			return false;
		}
		if (value.must()) {
			return sourceRefiner.merge(Tuple.of(source), TruthValue.TRUE) &&
					sourceRefiner.merge(Tuple.of(target), TruthValue.TRUE);
		}
		return true;
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol, PartialRelation sourceType) {
		return (adapter, partialSymbol) -> new UndirectedCrossReferenceRefiner(adapter, partialSymbol, concreteSymbol,
				sourceType);
	}
}
