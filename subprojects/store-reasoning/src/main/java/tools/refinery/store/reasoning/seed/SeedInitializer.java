/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.seed;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.representation.Symbol;

public class SeedInitializer<T> implements PartialModelInitializer {
	private final Symbol<T> symbol;
	private final Seed<T> seed;

	public SeedInitializer(Symbol<T> symbol, Seed<T> seed) {
		this.symbol = symbol;
		this.seed = seed;
	}

	@Override
	public void initialize(Model model, int nodeCount) {
		var interpretation = model.getInterpretation(symbol);
		var cursor = seed.getCursor(symbol.defaultValue(), nodeCount);
		interpretation.putAll(cursor);
	}
}
