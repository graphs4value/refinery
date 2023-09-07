/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.store.query.view.TuplePreservingView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

class MultiView extends TuplePreservingView<CardinalityInterval> {
	protected MultiView(Symbol<CardinalityInterval> symbol) {
		super(symbol, "multi");
	}

	@Override
	protected boolean doFilter(Tuple key, CardinalityInterval value) {
		return !CardinalityIntervals.ONE.equals(value);
	}
}
