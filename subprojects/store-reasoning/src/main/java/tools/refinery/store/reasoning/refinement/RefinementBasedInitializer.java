/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class RefinementBasedInitializer<A, C> implements PartialModelInitializer {
	private final PartialSymbol<A, C> partialSymbol;

	public RefinementBasedInitializer(PartialSymbol<A, C> partialSymbol) {
		this.partialSymbol = partialSymbol;
	}

	@Override
	public void initialize(Model model, ModelSeed modelSeed) {
		var refiner = model.getAdapter(ReasoningAdapter.class).getRefiner(partialSymbol);
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
