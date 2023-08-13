/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

class MustContainmentLinkView extends InferredContainmentLinkView {
	public MustContainmentLinkView(Symbol<InferredContainment> symbol, PartialRelation link) {
		super(symbol, "must", link);
	}

	@Override
	protected boolean doFilter(Tuple key, InferredContainment value) {
		return value.mustLinks().contains(link);
	}
}
