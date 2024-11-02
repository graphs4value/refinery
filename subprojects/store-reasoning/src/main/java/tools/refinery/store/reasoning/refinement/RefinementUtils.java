/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.reasoning.seed.ModelSeed;

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
}
