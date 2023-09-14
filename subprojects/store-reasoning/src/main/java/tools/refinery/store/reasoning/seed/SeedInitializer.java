/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.seed;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;

public class SeedInitializer<T> implements PartialModelInitializer {
	private final Symbol<T> symbol;
	private final PartialSymbol<T, ?> partialSymbol;

	public SeedInitializer(Symbol<T> symbol, PartialSymbol<T, ?> partialSymbol) {
		this.symbol = symbol;
		this.partialSymbol = partialSymbol;
	}

	@Override
	public void initialize(Model model, ModelSeed modelSeed) {
		var interpretation = model.getInterpretation(symbol);
		var cursor = modelSeed.getCursor(partialSymbol, symbol.defaultValue());
		interpretation.putAll(cursor);
	}
}
