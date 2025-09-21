/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.logic.term.Parameter;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.uppercardinality.FiniteUpperCardinality;
import tools.refinery.store.query.view.AbstractFunctionView;
import tools.refinery.store.representation.Symbol;

class CountIntervalCandidateView extends AbstractFunctionView<CardinalityInterval> {
	public CountIntervalCandidateView(Symbol<CardinalityInterval> symbol) {
		super(symbol, "intervalCandidate", new Parameter(IntInterval.class));
	}

	@Override
	protected Object forwardMapValue(CardinalityInterval value) {
		int lowerBound = value.lowerBound();
		int upperBound = lowerBound == 0 ? 0 : 1;
		if (value.upperBound() instanceof FiniteUpperCardinality(int finiteUpperBound)) {
			upperBound = Math.min(upperBound, finiteUpperBound);
		}
		return IntInterval.of(lowerBound, upperBound);
	}
}
