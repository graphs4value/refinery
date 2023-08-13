/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.query.view.TuplePreservingView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

class ForbiddenContainsView extends TuplePreservingView<InferredContainment> {
	public ForbiddenContainsView(Symbol<InferredContainment> symbol) {
		super(symbol, "contains#forbidden");
	}

	@Override
	protected boolean doFilter(Tuple key, InferredContainment value) {
		return !value.contains().may();
	}
}
