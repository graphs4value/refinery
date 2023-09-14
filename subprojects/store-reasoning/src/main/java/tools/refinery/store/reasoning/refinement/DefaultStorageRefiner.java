/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public class DefaultStorageRefiner<T> implements StorageRefiner {
	private static final StorageRefiner.Factory<Object> FACTORY = DefaultStorageRefiner::new;

	private final Interpretation<T> interpretation;

	public DefaultStorageRefiner(Symbol<T> symbol, Model model) {
		interpretation = model.getInterpretation(symbol);
	}

	@Override
	public boolean split(int parentNode, int childNode) {
		var symbol = interpretation.getSymbol();
		int arity = symbol.arity();
		for (int i = 0; i < arity; i++) {
			int adjacentSize = interpretation.getAdjacentSize(i, parentNode);
			if (adjacentSize == 0) {
				continue;
			}
			var toSetKeys = new Tuple[adjacentSize];
			// This is safe, because we won't pass the array to the outside.
			@SuppressWarnings("unchecked")
			var toSetValues = (T[]) new Object[adjacentSize];
			var cursor = interpretation.getAdjacent(i, parentNode);
			int j = 0;
			while (cursor.move()) {
				toSetKeys[j] = cursor.getKey().set(i, childNode);
				toSetValues[j] = cursor.getValue();
				j++;
			}
			for (j = 0; j < adjacentSize; j++) {
				interpretation.put(toSetKeys[j], toSetValues[j]);
			}
		}
		return true;
	}

	@Override
	public boolean cleanup(int nodeToDelete) {
		var symbol = interpretation.getSymbol();
		int arity = symbol.arity();
		var defaultValue = symbol.defaultValue();
		for (int i = 0; i < arity; i++) {
			int adjacentSize = interpretation.getAdjacentSize(i, nodeToDelete);
			if (adjacentSize == 0) {
				continue;
			}
			var toDelete = new Tuple[adjacentSize];
			var cursor = interpretation.getAdjacent(i, nodeToDelete);
			int j = 0;
			while (cursor.move()) {
				toDelete[j] = cursor.getKey();
				j++;
			}
			for (j = 0; j < adjacentSize; j++) {
				interpretation.put(toDelete[j], defaultValue);
			}
		}
		return true;
	}

	public static <T> StorageRefiner.Factory<T> factory() {
		// This is safe, because {@code FACTORY} doesn't depend on {@code T} at all.
		@SuppressWarnings("unchecked")
		var typedFactory = (StorageRefiner.Factory<T>) FACTORY;
		return typedFactory;
	}
}
