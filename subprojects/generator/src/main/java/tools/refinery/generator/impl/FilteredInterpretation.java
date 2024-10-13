/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.map.AnyVersionedMap;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public class FilteredInterpretation<A extends AbstractValue<A, C>, C> implements PartialInterpretation<A, C> {
	private final PartialInterpretation<A, C> wrappedInterpretation;
	private final PartialInterpretation<TruthValue, Boolean> existsInterpretation;

	public FilteredInterpretation(PartialInterpretation<A, C> wrappedInterpretation, PartialInterpretation<TruthValue,
			Boolean> existsInterpretation) {
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
		return new FilteredCursor(wrappedInterpretation.getAll());
	}

	private boolean tupleExists(Tuple key) {
		int arity = key.getSize();
		for (int i = 0; i < arity; i++) {
			if (!existsInterpretation.get(Tuple.of(key.get(i))).may()) {
				return false;
			}
		}
		return true;
	}

	private class FilteredCursor implements Cursor<Tuple, A> {
		private final Cursor<Tuple, A> wrappedCursor;

		private FilteredCursor(Cursor<Tuple, A> wrappedCursor) {
			this.wrappedCursor = wrappedCursor;
		}

		@Override
		public Tuple getKey() {
			return wrappedCursor.getKey();
		}

		@Override
		public A getValue() {
			return wrappedCursor.getValue();
		}

		@Override
		public boolean isTerminated() {
			return wrappedCursor.isTerminated();
		}

		@Override
		public boolean move() {
			while (wrappedCursor.move()) {
				if (tupleExists(getKey())) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean isDirty() {
			return wrappedCursor.isDirty();
		}

		@Override
		public Set<AnyVersionedMap> getDependingMaps() {
			return wrappedCursor.getDependingMaps();
		}
	}
}
