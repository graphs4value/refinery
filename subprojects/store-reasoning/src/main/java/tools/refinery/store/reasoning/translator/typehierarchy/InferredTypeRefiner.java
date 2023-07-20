/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.AbstractPartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

class InferredTypeRefiner extends AbstractPartialInterpretationRefiner<TruthValue, Boolean> {
	private final Interpretation<InferredType> interpretation;
	private final TypeAnalysisResult result;

	private InferredTypeRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
								Symbol<InferredType> symbol, TypeAnalysisResult result) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(symbol);
		this.result = result;
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var currentType = interpretation.get(key);
		var newType = result.merge(currentType, value);
		interpretation.put(key, newType);
		return true;
	}

	public static Factory<TruthValue, Boolean> of(Symbol<InferredType> symbol, TypeAnalysisResult result) {
		return (adapter, partialSymbol) -> new InferredTypeRefiner(adapter, partialSymbol, symbol, result);
	}
}
