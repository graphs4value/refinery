/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public class QueryBasedRelationInterpretationFactory implements PartialInterpretation.Factory<TruthValue, Boolean> {
	private final Query<Boolean> may;
	private final Query<Boolean> must;
	private final Query<Boolean> candidateMay;
	private final Query<Boolean> candidateMust;

	public QueryBasedRelationInterpretationFactory(
			Query<Boolean> may, Query<Boolean> must, Query<Boolean> candidateMay, Query<Boolean> candidateMust) {
		this.may = may;
		this.must = must;
		this.candidateMay = candidateMay;
		this.candidateMust = candidateMust;
	}

	@Override
	public PartialInterpretation<TruthValue, Boolean> create(
			ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValue, Boolean> partialSymbol) {
		var queryEngine = adapter.getModel().getAdapter(ModelQueryAdapter.class);
		ResultSet<Boolean> mayResultSet;
		ResultSet<Boolean> mustResultSet;
		switch (concreteness) {
		case PARTIAL -> {
			mayResultSet = queryEngine.getResultSet(may);
			mustResultSet = queryEngine.getResultSet(must);
		}
		case CANDIDATE -> {
			mayResultSet = queryEngine.getResultSet(candidateMay);
			mustResultSet = queryEngine.getResultSet(candidateMust);
		}
		default -> throw new IllegalArgumentException("Unknown concreteness: " + concreteness);
		}
		if (mayResultSet.equals(mustResultSet)) {
			return new TwoValuedInterpretation(adapter, concreteness, partialSymbol, mustResultSet);
		} else {
			return new FourValuedInterpretation(
					adapter, concreteness, partialSymbol, mayResultSet, mustResultSet);
		}
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder, Set<Concreteness> requiredInterpretations) {
		var queryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		if (requiredInterpretations.contains(Concreteness.PARTIAL)) {
			queryBuilder.queries(may, must);
		}
		if (requiredInterpretations.contains(Concreteness.CANDIDATE)) {
			queryBuilder.queries(candidateMay, candidateMust);
		}
	}

	private static class TwoValuedInterpretation extends AbstractPartialInterpretation<TruthValue, Boolean>
		implements ResultSetListener<Boolean> {
		private final ResultSet<Boolean> resultSet;

		protected TwoValuedInterpretation(
				ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValue, Boolean> partialSymbol,
				ResultSet<Boolean> resultSet) {
			super(adapter, concreteness, partialSymbol);
			this.resultSet = resultSet;
		}

		@Override
		public TruthValue get(Tuple key) {
			return TruthValue.of(resultSet.get(key));
		}

		@Override
		public Cursor<Tuple, TruthValue> getAll() {
			return new TwoValuedCursor(resultSet.getAll());
		}

		@Override
		protected void startListeningForChanges() {
			resultSet.addListener(this);
		}

		@Override
		protected void stopListeningForChanges() {
			resultSet.removeListener(this);
		}

		@Override
		public void put(Tuple key, Boolean fromValue, Boolean toValue) {
			notifyChange(key, TruthValue.of(fromValue), TruthValue.of(toValue));
		}

		private record TwoValuedCursor(Cursor<Tuple, Boolean> cursor) implements Cursor<Tuple, TruthValue> {
			@Override
			public Tuple getKey() {
				return cursor.getKey();
			}

			@Override
			public TruthValue getValue() {
				return TruthValue.of(cursor.getValue());
			}

			@Override
			public boolean isTerminated() {
				return cursor.isTerminated();
			}

			@Override
			public boolean move() {
				return cursor.move();
			}
		}
	}

	private static class FourValuedInterpretation extends AbstractPartialInterpretation<TruthValue, Boolean> {
		private final ResultSet<Boolean> mayResultSet;
		private final ResultSet<Boolean> mustResultSet;
		private ResultSetListener<Boolean> mayListener;
		private ResultSetListener<Boolean> mustListener;

		public FourValuedInterpretation(
				ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValue, Boolean> partialSymbol,
				ResultSet<Boolean> mayResultSet, ResultSet<Boolean> mustResultSet) {
			super(adapter, concreteness, partialSymbol);
			this.mayResultSet = mayResultSet;
			this.mustResultSet = mustResultSet;
		}

		@Override
		public TruthValue get(Tuple key) {
			boolean isMay = mayResultSet.get(key);
			boolean isMust = mustResultSet.get(key);
			return TruthValue.of(isMay, isMust);
		}

		@Override
		public Cursor<Tuple, TruthValue> getAll() {
			return new FourValuedCursor();
		}

		@Override
		protected void startListeningForChanges() {
			if (mayListener == null) {
				mayListener = (key, oldValue, newValue) -> {
					boolean mustValue = mustResultSet.get(key);
					notifyChange(key, TruthValue.of(oldValue, mustValue), TruthValue.of(newValue, mustValue));
				};
			}
			if (mustListener == null) {
				mustListener = (key, oldValue, newValue) -> {
					boolean mayValue = mayResultSet.get(key);
					notifyChange(key, TruthValue.of(mayValue, oldValue), TruthValue.of(mayValue, newValue));
				};
			}
			mayResultSet.addListener(mayListener);
			mustResultSet.addListener(mustListener);
		}

		@Override
		protected void stopListeningForChanges() {
			mayResultSet.removeListener(mayListener);
			mustResultSet.removeListener(mustListener);
		}

		private final class FourValuedCursor implements Cursor<Tuple, TruthValue> {
			private final Cursor<Tuple, Boolean> mayCursor;
			private Cursor<Tuple, Boolean> mustCursor;

			private FourValuedCursor() {
				this.mayCursor = mayResultSet.getAll();
			}

			@Override
			public Tuple getKey() {
				return mustCursor == null ? mayCursor.getKey() : mustCursor.getKey();
			}

			@Override
			public TruthValue getValue() {
				if (mustCursor != null) {
					return TruthValue.ERROR;
				}
				if (Boolean.TRUE.equals(mustResultSet.get(mayCursor.getKey()))) {
					return TruthValue.TRUE;
				}
				return TruthValue.UNKNOWN;
			}

			@Override
			public boolean isTerminated() {
				return mustCursor != null && mustCursor.isTerminated();
			}

			@Override
			public boolean move() {
				if (mayCursor.isTerminated()) {
					return moveMust();
				}
				if (mayCursor.move()) {
					return true;
				}
				mustCursor = mustResultSet.getAll();
				return moveMust();
			}

			private boolean moveMust() {
				while (mustCursor.move()) {
					// We already iterated over {@code TRUE} truth values with {@code mayCursor}.
					if (!Boolean.TRUE.equals(mayResultSet.get(mustCursor.getKey()))) {
						return true;
					}
				}
				return false;
			}
		}
	}
}
