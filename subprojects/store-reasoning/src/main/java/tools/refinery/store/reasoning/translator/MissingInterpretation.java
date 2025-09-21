/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.AbstractPartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

public class MissingInterpretation<A extends AbstractValue<A, C>, C>
		extends AbstractPartialInterpretation<A, C> {
	public MissingInterpretation(ReasoningAdapter adapter, Concreteness concreteness,
								 PartialSymbol<A, C> partialSymbol) {
		super(adapter, concreteness, partialSymbol);
	}

	@Override
	public A get(Tuple key) {
		return fail();
	}

	@Override
	public Cursor<Tuple, A> getAll() {
		return fail();
	}

	private <T> T fail() {
		throw new UnsupportedOperationException("No interpretation for shadow predicate: " + getPartialSymbol());
	}
}
