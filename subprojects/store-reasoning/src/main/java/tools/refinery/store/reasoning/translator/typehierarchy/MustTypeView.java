/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

class MustTypeView extends InferredTypeView {
	public MustTypeView(Symbol<InferredType> symbol, PartialRelation type) {
		super(symbol, "must", type);
	}

	@Override
	protected boolean doFilter(Tuple key, InferredType value) {
		return value.mustTypes().contains(type);
	}
}
