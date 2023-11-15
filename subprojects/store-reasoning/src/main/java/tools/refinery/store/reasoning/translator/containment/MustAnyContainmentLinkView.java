/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.query.view.TuplePreservingView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

class MustAnyContainmentLinkView extends TuplePreservingView<InferredContainment> {
	public MustAnyContainmentLinkView(Symbol<InferredContainment> symbol) {
		super(symbol, "contains#mustAnyLink");
	}

	@Override
	protected boolean doFilter(Tuple key, InferredContainment value) {
		return !value.mustLinks().isEmpty();
	}
}
