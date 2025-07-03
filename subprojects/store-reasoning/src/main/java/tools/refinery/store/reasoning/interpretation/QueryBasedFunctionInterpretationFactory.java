/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.FilteredCursor;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public class QueryBasedFunctionInterpretationFactory<A extends AbstractValue<A, C>, C>
		implements PartialInterpretation.Factory<A, C> {
	private final FunctionalQuery<A> partial;
	private final FunctionalQuery<A> candidate;
	private final A errorValue;

	public QueryBasedFunctionInterpretationFactory(FunctionalQuery<A> partial, FunctionalQuery<A> candidate,
												   AbstractDomain<A, C> abstractDomain) {
		this.partial = partial;
		this.candidate = candidate;
		this.errorValue = abstractDomain.error();
	}

	@Override
	public PartialInterpretation<A, C> create(
			ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<A, C> partialSymbol) {
		var queryEngine = adapter.getModel().getAdapter(ModelQueryAdapter.class);
		var resultSet = switch (concreteness) {
			case PARTIAL -> queryEngine.getResultSet(partial);
			case CANDIDATE -> queryEngine.getResultSet(candidate);
		};
		return new FunctionInterpretation<>(adapter, concreteness, partialSymbol, resultSet, errorValue);
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder, Set<Concreteness> requiredInterpretations) {
		var queryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		if (requiredInterpretations.contains(Concreteness.PARTIAL)) {
			queryBuilder.query(partial);
		}
		if (requiredInterpretations.contains(Concreteness.CANDIDATE)) {
			queryBuilder.query(candidate);
		}
	}

	private static class FunctionInterpretation<A extends AbstractValue<A, C>, C>
			extends AbstractPartialInterpretation<A, C> {
		private final ResultSet<A> resultSet;
		private final A errorValue;

		protected FunctionInterpretation(ReasoningAdapter adapter, Concreteness concreteness,
										 PartialSymbol<A, C> partialSymbol, ResultSet<A> resultSet, A errorValue) {
			super(adapter, concreteness, partialSymbol);
			this.resultSet = resultSet;
			this.errorValue = errorValue;
		}

		@Override
		public A get(Tuple key) {
			var result = resultSet.get(key);
			return result == null ? errorValue : result;
		}

		@Override
		public Cursor<Tuple, A> getAll() {
			return new FilteredResultSetCursor<>(resultSet.getAll(), errorValue);
		}
	}

	private static class FilteredResultSetCursor<A> extends FilteredCursor<Tuple, A> {
		private final A errorValue;

		public FilteredResultSetCursor(Cursor<Tuple, A> wrappedCursor, A errorValue) {
			super(wrappedCursor);
			this.errorValue = errorValue;
		}

		@Override
		protected boolean keep() {
			return !errorValue.equals(getValue());
		}
	}
}
