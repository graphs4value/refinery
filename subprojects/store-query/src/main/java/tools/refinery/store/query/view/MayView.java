/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

public class MayView extends TuplePreservingView<TruthValue> {
	public MayView(Symbol<TruthValue> symbol) {
		super(symbol, "may");
	}

	@Override
	protected boolean doFilter(Tuple key, TruthValue value) {
		return value.may();
	}
}
