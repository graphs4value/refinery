/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.query.term.DataSort;
import tools.refinery.store.query.term.Sort;
import tools.refinery.store.representation.Symbol;

public final class FunctionView<T> extends AbstractFunctionView<T> {
	public FunctionView(Symbol<T> symbol, String name) {
		super(symbol, name);
	}

	public FunctionView(Symbol<T> symbol) {
		this(symbol, "function");
	}

	@Override
	protected Sort getForwardMappedValueSort() {
		return new DataSort<>(getSymbol().valueType());
	}
}
