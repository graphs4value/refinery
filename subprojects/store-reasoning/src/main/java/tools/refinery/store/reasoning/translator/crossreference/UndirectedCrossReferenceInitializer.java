/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.LinkedHashMap;

class UndirectedCrossReferenceInitializer implements PartialModelInitializer {
	private final PartialRelation linkType;
	private final Symbol<TruthValue> symbol;

	UndirectedCrossReferenceInitializer(PartialRelation linkType, Symbol<TruthValue> symbol) {
		this.linkType = linkType;
		this.symbol = symbol;
	}

	@Override
	public void initialize(Model model, ModelSeed modelSeed) {
		var mergedMap = getMergedMap(modelSeed);
		var interpretation = model.getInterpretation(symbol);
		for (var entry : mergedMap.entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue();
			interpretation.put(key, value);
		}
	}

	@NotNull
	private LinkedHashMap<Tuple, TruthValue> getMergedMap(ModelSeed modelSeed) {
		var defaultValue = symbol.defaultValue();
		var originalMap = new LinkedHashMap<Tuple, TruthValue>();
		var cursor = modelSeed.getCursor(linkType, defaultValue);
		while (cursor.move()) {
			if (originalMap.put(cursor.getKey(), cursor.getValue()) != null) {
				throw new TranslationException(linkType, "Duplicate value for key " + cursor.getKey());
			}
		}
		var mergedMap = LinkedHashMap.<Tuple, TruthValue>newLinkedHashMap(originalMap.size());
		for (var entry : originalMap.entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue();
			int first = key.get(0);
			int second = key.get(1);
			var oppositeKey = Tuple.of(second, first);
			var oppositeValue = originalMap.get(oppositeKey);
			if (oppositeValue != null && second < first) {
				// Already processed entry.
				continue;
			}
			var mergedValue = value.meet(oppositeValue == null ? defaultValue : oppositeValue);
			mergedMap.put(key, mergedValue);
			if (first != second) {
				mergedMap.put(oppositeKey, mergedValue);
			}
		}
		return mergedMap;
	}
}
