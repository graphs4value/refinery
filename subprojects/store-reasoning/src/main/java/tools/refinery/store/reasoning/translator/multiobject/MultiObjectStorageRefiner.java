/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

class MultiObjectStorageRefiner implements StorageRefiner {
	private final Interpretation<CardinalityInterval> countInterpretation;

	public MultiObjectStorageRefiner(Symbol<CardinalityInterval> countSymbol, Model model) {
		countInterpretation = model.getInterpretation(countSymbol);
	}

	@Override
	public boolean split(int parentNode, int childNode) {
		var parentKey = Tuple.of(parentNode);
		var parentCount = countInterpretation.get(parentKey);
		if (parentCount == null) {
			return false;
		}
		var newParentCount = parentCount.take(1);
		if (newParentCount.isEmpty()) {
			return false;
		}
		var childKey = Tuple.of(childNode);
		countInterpretation.put(parentKey, newParentCount);
		countInterpretation.put(childKey, CardinalityIntervals.ONE);
		return true;
	}

	@Override
	public boolean cleanup(int nodeToDelete) {
		var previousCount = countInterpretation.put(Tuple.of(nodeToDelete), null);
		return previousCount.lowerBound() == 0;
	}
}
