/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.opposite;


import tools.refinery.store.map.AnyVersionedMap;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.AbstractPartialInterpretation;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

class OppositeInterpretation<A, C> extends AbstractPartialInterpretation<A, C> {
	private final PartialInterpretation<A, C> opposite;

	private OppositeInterpretation(ReasoningAdapter adapter, Concreteness concreteness,
								   PartialSymbol<A, C> partialSymbol, PartialInterpretation<A, C> opposite) {
		super(adapter, concreteness, partialSymbol);
		this.opposite = opposite;
	}

	@Override
	public A get(Tuple key) {
		return opposite.get(OppositeUtils.flip(key));
	}

	@Override
	public Cursor<Tuple, A> getAll() {
		return new OppositeCursor<>(opposite.getAll());
	}

	public static <A1, C1> Factory<A1, C1> of(PartialSymbol<A1, C1> oppositeSymbol) {
		return (adapter, concreteness, partialSymbol) -> {
			var opposite = adapter.getPartialInterpretation(concreteness, oppositeSymbol);
			return new OppositeInterpretation<>(adapter, concreteness, partialSymbol, opposite);
		};
	}

	private record OppositeCursor<T>(Cursor<Tuple, T> opposite) implements Cursor<Tuple, T> {
		@Override
		public Tuple getKey() {
			return OppositeUtils.flip(opposite.getKey());
		}

		@Override
		public T getValue() {
			return opposite.getValue();
		}

		@Override
		public boolean isTerminated() {
			return opposite.isTerminated();
		}

		@Override
		public boolean move() {
			return opposite.move();
		}

		@Override
		public Set<AnyVersionedMap> getDependingMaps() {
			return opposite.getDependingMaps();
		}

		@Override
		public boolean isDirty() {
			return opposite.isDirty();
		}
	}
}
