/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public final class RefinementUtils {
	private RefinementUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static <A extends AbstractValue<A, C>, C> void refineFromSeed(PartialInterpretationRefiner<A, C> refiner,
																		 ModelSeed modelSeed) {
		var partialSymbol = refiner.getPartialSymbol();
		var defaultValue = partialSymbol.abstractDomain().unknown();
		var cursor = modelSeed.getCursor(partialSymbol, defaultValue);
		while (cursor.move()) {
			var key = cursor.getKey();
			var value = cursor.getValue();
			if (!refiner.merge(key, value)) {
				throw new IllegalArgumentException("Failed to merge value %s for key %s into %s"
						.formatted(value, key, partialSymbol));
			}
		}
	}

	public static <A extends AbstractValue<A, C>, C> boolean mergeAll(
			PartialInterpretationRefiner<A, C>[] refiners, Tuple key, A value) {
		int count = refiners.length;
		// Use classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < count; i++) {
			var refiner = refiners[i];
			if (!refiner.merge(key, value)) {
				return false;
			}
		}
		return true;
	}

	public static <A extends AbstractValue<A, C>, C> PartialInterpretationRefiner<A, C>[] getRefiners(
			ReasoningAdapter adapter, Set<? extends PartialSymbol<A, C>> relations) {
		// Creation of array with generic member type.
		@SuppressWarnings("unchecked")
		var refiners = (PartialInterpretationRefiner<A, C>[]) new PartialInterpretationRefiner<?, ?>[relations.size()];
		var i = 0;
		for (var relation : relations) {
			refiners[i] = adapter.getRefiner(relation);
			i++;
		}
		return refiners;
	}
}
