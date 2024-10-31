/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteRelationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.reasoning.translator.TranslatorUtils;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;
import java.util.Set;

class UndirectedCrossReferenceRefiner extends ConcreteRelationRefiner {
	private final PartialRelation sourceType;
	private final Set<PartialRelation> supersets;
	private PartialInterpretationRefiner<TruthValue, Boolean> sourceRefiner;
	private PartialInterpretationRefiner<TruthValue, Boolean>[] supersetRefiners;

	protected UndirectedCrossReferenceRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, UndirectedCrossReferenceInfo info, RoundingMode roundingMode) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode);
		this.sourceType = info.type();
		this.supersets = info.supersets();
	}

	@Override
	public void afterCreate() {
		var adapter = getAdapter();
		sourceRefiner = adapter.getRefiner(sourceType);
		supersetRefiners = TranslatorUtils.getRefiners(adapter, supersets);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		int source = key.get(0);
		int target = key.get(1);
		var currentValue = get(key);
		var mergedValue = concretizationAwareMeet(currentValue, value);
		if (!Objects.equals(currentValue, mergedValue)) {
			var oldValue = put(key, mergedValue);
			if (source != target) {
				var inverseOldValue = put(Tuple.of(target, source), mergedValue);
				if (!Objects.equals(oldValue, inverseOldValue)) {
					return false;
				}
			}
		}
		if (value.must()) {
			return sourceRefiner.merge(Tuple.of(source), TruthValue.TRUE) && sourceRefiner.merge(Tuple.of(target),
					TruthValue.TRUE) && mergeSupersets(key);
		}
		return true;
	}

	private boolean mergeSupersets(Tuple key) {
		return TranslatorUtils.mergeAll(supersetRefiners, supersetRefiners, key, TruthValue.TRUE);
	}

	@Override
	public void afterInitialize(ModelSeed modelSeed) {
		var linkType = getPartialSymbol();
		var cursor = modelSeed.getCursor(linkType);
		while (cursor.move()) {
			var value = cursor.getValue();
			if (value.must()) {
				var key = cursor.getKey();
				boolean mergedTypes =
						sourceRefiner.merge(Tuple.of(key.get(0)), TruthValue.TRUE) && sourceRefiner.merge(Tuple.of(key.get(1)), TruthValue.TRUE);
				if (!mergedTypes) {
					throw new IllegalArgumentException("Failed to merge end types of reference %s for key %s".formatted(linkType, key));
				}
				if (!mergeSupersets(key)) {
					throw new IllegalArgumentException("Failed to merge supersets of reference %s for key %s".formatted(linkType, key));
				}
			}
		}
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol, UndirectedCrossReferenceInfo info,
												  RoundingMode roundingMode) {
		return (adapter, partialSymbol) -> new UndirectedCrossReferenceRefiner(adapter, partialSymbol, concreteSymbol,
				info, roundingMode);
	}
}
