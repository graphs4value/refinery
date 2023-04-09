/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.internal;

import tools.refinery.store.representation.Symbol;

public record SymbolEquivalenceClass<T>(int arity, Class<T> valueType, T defaultValue) {
	public SymbolEquivalenceClass(Symbol<T> symbol) {
		this(symbol.arity(), symbol.valueType(), symbol.defaultValue());
	}
}
