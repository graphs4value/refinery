/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.query.view.AbstractFunctionView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.UpperCardinality;

class UpperCardinalityView extends AbstractFunctionView<CardinalityInterval> {
	public UpperCardinalityView(Symbol<CardinalityInterval> symbol) {
		super(symbol, "upper", new Parameter(UpperCardinality.class));
	}

	@Override
	protected Object forwardMapValue(CardinalityInterval value) {
		return value.upperBound();
	}
}
