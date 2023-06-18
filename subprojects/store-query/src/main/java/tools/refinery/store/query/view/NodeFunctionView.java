/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple1;

public final class NodeFunctionView extends AbstractFunctionView<Tuple1> {
	public NodeFunctionView(Symbol<Tuple1> symbol, String name) {
		super(symbol, name, Parameter.NODE_OUT);
	}

	public NodeFunctionView(Symbol<Tuple1> symbol) {
		this(symbol, "function");
	}
}
