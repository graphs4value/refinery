/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.logic.term.Parameter;
import tools.refinery.logic.term.intinterval.IntBound;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.store.query.view.AbstractFunctionView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;

class CountIntervalView extends AbstractFunctionView<CardinalityInterval> {
	public CountIntervalView(Symbol<CardinalityInterval> symbol) {
		super(symbol, "interval", new Parameter(IntInterval.class));
	}

	@Override
	protected Object forwardMapValue(CardinalityInterval value) {
		var upperBound = value.upperBound() instanceof FiniteUpperCardinality(int finiteUpperBound) ?
				IntBound.of(finiteUpperBound) : IntBound.Infinite.POSITIVE_INFINITY;
		return IntInterval.of(value.lowerBound(), upperBound);
	}
}
