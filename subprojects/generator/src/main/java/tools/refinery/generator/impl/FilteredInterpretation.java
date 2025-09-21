/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.FilteredCursor;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class FilteredInterpretation<A extends AbstractValue<A, C>, C> implements PartialInterpretation<A, C> {
	private final PartialInterpretation<A, C> wrappedInterpretation;
	private final PartialInterpretation<TruthValue, Boolean> existsInterpretation;

	private FilteredInterpretation(PartialInterpretation<A, C> wrappedInterpretation,
								   PartialInterpretation<TruthValue, Boolean> existsInterpretation) {
		this.wrappedInterpretation = wrappedInterpretation;
		this.existsInterpretation = existsInterpretation;
	}

	@Override
	public ReasoningAdapter getAdapter() {
		return wrappedInterpretation.getAdapter();
	}

	@Override
	public PartialSymbol<A, C> getPartialSymbol() {
		return wrappedInterpretation.getPartialSymbol();
	}

	@Override
	public Concreteness getConcreteness() {
		return wrappedInterpretation.getConcreteness();
	}

	@Override
	public A get(Tuple key) {
		return tupleExists(key) ? wrappedInterpretation.get(key) :
				wrappedInterpretation.getPartialSymbol().defaultValue();
	}

	@Override
	public Cursor<Tuple, A> getAll() {
		return new FilteredInterpretationCursor(wrappedInterpretation.getAll());
	}

	private boolean tupleExists(Tuple key) {
		int arity = key.getSize();
		for (int i = 0; i < arity; i++) {
			if (existsInterpretation.get(Tuple.of(key.get(i))) == TruthValue.FALSE) {
				return false;
			}
		}
		return true;
	}

	public static <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> of(
			PartialInterpretation<A, C> wrappedInterpretation,
			PartialInterpretation<TruthValue, Boolean> existsInterpretation) {
		if (Objects.equals(wrappedInterpretation, existsInterpretation)) {
			return wrappedInterpretation;
		}
		if (wrappedInterpretation instanceof FilteredInterpretation<A, C> filteredWrapped &&
				Objects.equals(filteredWrapped.existsInterpretation, existsInterpretation)) {
			return wrappedInterpretation;
		}
		return new FilteredInterpretation<>(wrappedInterpretation, existsInterpretation);
	}

	private class FilteredInterpretationCursor extends FilteredCursor<Tuple, A> {
		private FilteredInterpretationCursor(Cursor<Tuple, A> wrappedCursor) {
			super(wrappedCursor);
		}

		@Override
		protected boolean keep() {
			return tupleExists(getKey());
		}
	}
}
