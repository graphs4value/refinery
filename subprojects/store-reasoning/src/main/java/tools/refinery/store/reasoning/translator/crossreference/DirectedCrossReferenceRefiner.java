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

class DirectedCrossReferenceRefiner extends ConcreteSymbolRefiner<TruthValue, Boolean> {
	private final PartialInterpretationRefiner<TruthValue, Boolean> sourceRefiner;
	private final PartialInterpretationRefiner<TruthValue, Boolean> targetRefiner;

	public DirectedCrossReferenceRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
										 Symbol<TruthValue> concreteSymbol, PartialRelation sourceType,
										 PartialRelation targetType) {
		super(adapter, partialSymbol, concreteSymbol);
		sourceRefiner = adapter.getRefiner(sourceType);
		targetRefiner = adapter.getRefiner(targetType);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		if (!super.merge(key, value)) {
			return false;
		}
		if (value.must()) {
			return sourceRefiner.merge(Tuple.of(key.get(0)), TruthValue.TRUE) &&
					targetRefiner.merge(Tuple.of(key.get(1)), TruthValue.TRUE);
		}
		return true;
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol, PartialRelation sourceType,
												  PartialRelation targetType) {
		return (adapter, partialSymbol) -> new DirectedCrossReferenceRefiner(adapter, partialSymbol, concreteSymbol,
				sourceType, targetType);
	}
}
