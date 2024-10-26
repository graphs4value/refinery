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
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

class DirectedCrossReferenceRefiner extends ConcreteRelationRefiner {
	private final PartialRelation sourceType;
	private final PartialRelation targetType;
	private final Set<PartialRelation> supersets;
	private final Set<PartialRelation> oppositeSupersets;
	private PartialInterpretationRefiner<TruthValue, Boolean> sourceRefiner;
	private PartialInterpretationRefiner<TruthValue, Boolean> targetRefiner;
	private PartialInterpretationRefiner<TruthValue, Boolean>[] supersetRefiners;
	private PartialInterpretationRefiner<TruthValue, Boolean>[] oppositeSupersetRefiners;

	protected DirectedCrossReferenceRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, DirectedCrossReferenceInfo info, RoundingMode roundingMode) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode);
		this.sourceType = info.sourceType();
		this.targetType = info.targetType();
		this.supersets = info.supersets();
		this.oppositeSupersets = info.oppositeSupersets();
	}

	@Override
	public void afterCreate() {
		var adapter = getAdapter();
		sourceRefiner = adapter.getRefiner(sourceType);
		targetRefiner = adapter.getRefiner(targetType);
		supersetRefiners = getSupersetRefiners(supersets);
		oppositeSupersetRefiners = getSupersetRefiners(oppositeSupersets);
	}

	private PartialInterpretationRefiner<TruthValue, Boolean>[] getSupersetRefiners(Set<PartialRelation> relations) {
		// Creation of array with generic member type.
		@SuppressWarnings("unchecked") var refiners =
				(PartialInterpretationRefiner<TruthValue, Boolean>[]) new PartialInterpretationRefiner<?, ?>[relations.size()];
		var i = 0;
		for (var relation : relations) {
			refiners[i] = getAdapter().getRefiner(relation);
			i++;
		}
		return refiners;
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		if (!super.merge(key, value)) {
			return false;
		}
		if (value.must()) {
			return sourceRefiner.merge(Tuple.of(key.get(0)), TruthValue.TRUE) &&
					targetRefiner.merge(Tuple.of(key.get(1)), TruthValue.TRUE) &&
					mergeSupersets(key);
		}
		return true;
	}

	private boolean mergeSupersets(Tuple key) {
		// Use classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < supersetRefiners.length; i++) {
			if (!supersetRefiners[i].merge(key, TruthValue.TRUE)) {
				return false;
			}
		}
		if (oppositeSupersetRefiners.length > 0) {
			var oppositeKey = Tuple.of(key.get(1), key.get(0));
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < oppositeSupersetRefiners.length; i++) {
				if (!oppositeSupersetRefiners[i].merge(oppositeKey, TruthValue.TRUE)) {
					return false;
				}
			}
		}
		return true;
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
						sourceRefiner.merge(Tuple.of(key.get(0)), TruthValue.TRUE) && targetRefiner.merge(Tuple.of(key.get(1)), TruthValue.TRUE);
				if (!mergedTypes) {
					throw new IllegalArgumentException("Failed to merge end types of reference %s for key %s".formatted(linkType, key));
				}
				if (!mergeSupersets(key)) {
					throw new IllegalArgumentException("Failed to merge supersets of reference %s for key %s".formatted(linkType, key));
				}
			}
		}
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol, DirectedCrossReferenceInfo info,
												  RoundingMode roundingMode) {
		return (adapter, partialSymbol) -> new DirectedCrossReferenceRefiner(adapter, partialSymbol, concreteSymbol,
				info, roundingMode);
	}
}
