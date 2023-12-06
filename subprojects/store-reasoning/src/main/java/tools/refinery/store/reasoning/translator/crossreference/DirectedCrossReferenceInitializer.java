/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

class DirectedCrossReferenceInitializer implements PartialModelInitializer {
	private final PartialRelation linkType;
	private final PartialRelation sourceType;
	private final PartialRelation targetType;
	private final Symbol<TruthValue> symbol;

	public DirectedCrossReferenceInitializer(PartialRelation linkType, PartialRelation sourceType,
											 PartialRelation targetType, Symbol<TruthValue> symbol) {
		this.linkType = linkType;
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.symbol = symbol;
	}

	@Override
	public void initialize(Model model, ModelSeed modelSeed) {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var sourceRefiner = reasoningAdapter.getRefiner(sourceType);
		var targetRefiner = reasoningAdapter.getRefiner(targetType);
		var interpretation = model.getInterpretation(symbol);
		var cursor = modelSeed.getCursor(linkType, symbol.defaultValue());
		while (cursor.move()) {
			var key = cursor.getKey();
			var value = cursor.getValue();
			interpretation.put(key, value);
			if (value.must()) {
				boolean merged = sourceRefiner.merge(Tuple.of(key.get(0)), TruthValue.TRUE) &&
						targetRefiner.merge(Tuple.of(key.get(1)), TruthValue.TRUE);
				if (!merged) {
					throw new IllegalArgumentException("Failed to merge end types of reference %s for key %s"
							.formatted(linkType, key));
				}
			}
		}
	}
}
