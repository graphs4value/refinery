/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public class TypeConstraintRefiner {
	private final PartialInterpretationRefiner<TruthValue, Boolean> sourceRefiner;
	private final PartialInterpretationRefiner<TruthValue, Boolean> targetRefiner;
	private final PartialInterpretationRefiner<TruthValue, Boolean>[] supersetRefiners;
	private final PartialInterpretationRefiner<TruthValue, Boolean>[] oppositeSupersetRefiners;

	public TypeConstraintRefiner(ReasoningAdapter adapter, PartialRelation sourceType, PartialRelation targetType,
								 Set<PartialRelation> supersets, Set<PartialRelation> oppositeSupersets) {
		sourceRefiner = adapter.getRefiner(sourceType);
		targetRefiner = adapter.getRefiner(targetType);
		supersetRefiners = RefinementUtils.getRefiners(adapter, supersets);
		oppositeSupersetRefiners = RefinementUtils.getRefiners(adapter, oppositeSupersets);
	}

	public boolean merge(Tuple key) {
		int source = key.get(0);
		int target = key.get(1);
		if (!sourceRefiner.merge(Tuple.of(source), TruthValue.TRUE) ||
				!targetRefiner.merge(Tuple.of(target), TruthValue.TRUE) ||
				!mergeSupersets(supersetRefiners, key)) {
			return false;
		}
		if (oppositeSupersetRefiners.length > 0) {
			var oppositeKey = Tuple.of(target, source);
			return mergeSupersets(oppositeSupersetRefiners, oppositeKey);
		}
		return true;
	}

	public void mergeFromSeed(PartialSymbol<TruthValue, Boolean> linkType, ModelSeed modelSeed) {
		var cursor = modelSeed.getCursor(linkType);
		while (cursor.move()) {
			var value = cursor.getValue();
			if (value.must()) {
				var key = cursor.getKey();
				if (!merge(key)) {
					throw new IllegalArgumentException("Failed to merge type constraints of %s for key %s"
							.formatted(linkType, key));
				}
			}
		}
	}

	private static boolean mergeSupersets(PartialInterpretationRefiner<TruthValue, Boolean>[] refiners, Tuple key) {
		return RefinementUtils.mergeAll(refiners, key, TruthValue.TRUE);
	}
}
