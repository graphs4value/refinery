/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.resultset.OrderedResultSet;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class OrderedResultSetTest {
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);

	@Test
	void relationalFlushTest() {
		var query = Query.of("Relation", (builder, p1, p2) -> builder.clause(
				 friendView.call(p1, p2)
		));

		var store = ModelStore.builder()
				.symbols(friend)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var resultSet = queryEngine.getResultSet(query);

		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 2), true);
		friendInterpretation.put(Tuple.of(1, 1), true);
		queryEngine.flushChanges();

		try (var orderedResultSet = new OrderedResultSet<>(resultSet)) {
			assertThat(orderedResultSet.size(), is(3));
			assertThat(orderedResultSet.getKey(0), is(Tuple.of(0, 1)));
			assertThat(orderedResultSet.getKey(1), is(Tuple.of(1, 1)));
			assertThat(orderedResultSet.getKey(2), is(Tuple.of(1, 2)));

			friendInterpretation.put(Tuple.of(1, 2), false);
			friendInterpretation.put(Tuple.of(0, 2), true);
			queryEngine.flushChanges();

			assertThat(orderedResultSet.size(), is(3));
			assertThat(orderedResultSet.getKey(0), is(Tuple.of(0, 1)));
			assertThat(orderedResultSet.getKey(1), is(Tuple.of(0, 2)));
			assertThat(orderedResultSet.getKey(2), is(Tuple.of(1, 1)));
		}
	}

	@Test
	void functionalFlushTest() {
		var query = Query.of("Function", Integer.class, (builder, p1, output) -> builder.clause(
				friendView.call(p1, Variable.of()),
				output.assign(friendView.count(p1, Variable.of()))
		));

		var store = ModelStore.builder()
				.symbols(friend)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var resultSet = queryEngine.getResultSet(query);

		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 2), true);
		friendInterpretation.put(Tuple.of(1, 1), true);
		queryEngine.flushChanges();

		try (var orderedResultSet = new OrderedResultSet<>(resultSet)) {
			assertThat(orderedResultSet.size(), is(2));
			assertThat(orderedResultSet.getKey(0), is(Tuple.of(0)));
			assertThat(orderedResultSet.getKey(1), is(Tuple.of(1)));

			friendInterpretation.put(Tuple.of(0, 1), false);
			friendInterpretation.put(Tuple.of(2, 1), true);
			queryEngine.flushChanges();

			assertThat(orderedResultSet.size(), is(2));
			assertThat(orderedResultSet.getKey(0), is(Tuple.of(1)));
			assertThat(orderedResultSet.getKey(1), is(Tuple.of(2)));

			friendInterpretation.put(Tuple.of(1, 1), false);
			queryEngine.flushChanges();

			assertThat(orderedResultSet.size(), is(2));
			assertThat(orderedResultSet.getKey(0), is(Tuple.of(1)));
			assertThat(orderedResultSet.getKey(1), is(Tuple.of(2)));

			friendInterpretation.put(Tuple.of(1, 2), false);
			friendInterpretation.put(Tuple.of(1, 0), true);
			queryEngine.flushChanges();

			assertThat(orderedResultSet.size(), is(2));
			assertThat(orderedResultSet.getKey(0), is(Tuple.of(1)));
			assertThat(orderedResultSet.getKey(1), is(Tuple.of(2)));
		}
	}
}
