/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.PriorityAgenda;
import tools.refinery.store.query.resultset.PriorityResultSet;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class PriorityResultSetTest {
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);
	private static final Symbol<Boolean> enemy = Symbol.of("enemy", 2);
	private static final AnySymbolView enemyView = new KeyOnlyView<>(enemy);

	@Test
	void relationalPriorityTest() {
		var query1 = Query.of("Relation", (builder, p1, p2) -> builder.clause(
				friendView.call(p1, p2)
		));
		var query2 = Query.of("Relation", (builder, p1, p2) -> builder.clause(
				enemyView.call(p1, p2)
		));
		var store = ModelStore.builder()
				.symbols(friend, enemy)
				.with(QueryInterpreterAdapter.builder()
						.queries(query1, query2))
				.build();
		var agenda = new PriorityAgenda();

		try (var model = store.createEmptyModel()) {
			var friendInterpretation = model.getInterpretation(friend);
			var enemyInterpretation = model.getInterpretation(enemy);
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var resultSet1 = queryEngine.getResultSet(query1);
			var resultSet2 = queryEngine.getResultSet(query2);

			friendInterpretation.put(Tuple.of(0, 1), true);
			enemyInterpretation.put(Tuple.of(0, 2), true);
			queryEngine.flushChanges();

			try (var highPriorityResultSet = PriorityResultSet.of(resultSet1, 1, agenda)) {
				try (var lowPriorityResultSet = PriorityResultSet.of(resultSet2, 0, agenda)) {
					assertThat(agenda.getHighestPriority(), is(1));

					assertThat(highPriorityResultSet.size(), is(1));
					assertThat(highPriorityResultSet.getKey(0), is(Tuple.of(0, 1)));

					assertThat(lowPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> lowPriorityResultSet.getKey(0));

					friendInterpretation.put(Tuple.of(0, 1), false);
					queryEngine.flushChanges();

					assertThat(agenda.getHighestPriority(), is(0));

					assertThat(highPriorityResultSet.size(), is(0));
					assertThrows(IndexOutOfBoundsException.class, () -> highPriorityResultSet.getKey(0));

					assertThat(lowPriorityResultSet.size(), is(1));
					assertThat(lowPriorityResultSet.getKey(0), is(Tuple.of(0, 2)));

					friendInterpretation.put(Tuple.of(0, 1), true);
					queryEngine.flushChanges();

					assertThat(agenda.getHighestPriority(), is(1));

					assertThat(highPriorityResultSet.size(), is(1));
					assertThat(highPriorityResultSet.getKey(0), is(Tuple.of(0, 1)));

					assertThat(lowPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> lowPriorityResultSet.getKey(0));

					friendInterpretation.put(Tuple.of(0, 1), false);
					enemyInterpretation.put(Tuple.of(0, 2), false);
					queryEngine.flushChanges();

					assertThat(agenda.getHighestPriority(), is(Integer.MIN_VALUE));

					assertThat(highPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> highPriorityResultSet.getKey(0));

					assertThat(lowPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> lowPriorityResultSet.getKey(0));
				}
			}
		}
	}

	@Test
	void functionalPriorityTest() {
		var query1 = Query.of("Function", Integer.class, (builder, p1, output) -> builder.clause(
				friendView.call(p1, Variable.of()),
				output.assign(friendView.count(p1, Variable.of()))
		));
		var query2 = Query.of("Function", Integer.class, (builder, p1, output) -> builder.clause(
				enemyView.call(p1, Variable.of()),
				output.assign(enemyView.count(p1, Variable.of()))
		));
		var store = ModelStore.builder()
				.symbols(friend, enemy)
				.with(QueryInterpreterAdapter.builder()
						.queries(query1, query2))
				.build();
		var agenda = new PriorityAgenda();

		try (var model = store.createEmptyModel()) {
			var friendInterpretation = model.getInterpretation(friend);
			var enemyInterpretation = model.getInterpretation(enemy);
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var resultSet1 = queryEngine.getResultSet(query1);
			var resultSet2 = queryEngine.getResultSet(query2);

			friendInterpretation.put(Tuple.of(0, 1), true);
			enemyInterpretation.put(Tuple.of(0, 2), true);
			queryEngine.flushChanges();

			try (var highPriorityResultSet = PriorityResultSet.of(resultSet1, 1, agenda)) {
				try (var lowPriorityResultSet = PriorityResultSet.of(resultSet2, 0, agenda)) {
					assertThat(agenda.getHighestPriority(), is(1));

					assertThat(highPriorityResultSet.size(), is(1));
					assertThat(highPriorityResultSet.getKey(0), is(Tuple.of(0)));

					assertThat(lowPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> lowPriorityResultSet.getKey(0));

					friendInterpretation.put(Tuple.of(0, 1), false);
					queryEngine.flushChanges();

					assertThat(agenda.getHighestPriority(), is(0));

					assertThat(highPriorityResultSet.size(), is(0));
					assertThrows(IndexOutOfBoundsException.class, () -> highPriorityResultSet.getKey(0));

					assertThat(lowPriorityResultSet.size(), is(1));
					assertThat(lowPriorityResultSet.getKey(0), is(Tuple.of(0)));

					friendInterpretation.put(Tuple.of(0, 1), true);
					queryEngine.flushChanges();

					assertThat(agenda.getHighestPriority(), is(1));

					assertThat(highPriorityResultSet.size(), is(1));
					assertThat(highPriorityResultSet.getKey(0), is(Tuple.of(0)));

					assertThat(lowPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> lowPriorityResultSet.getKey(0));

					friendInterpretation.put(Tuple.of(0, 1), false);
					enemyInterpretation.put(Tuple.of(0, 2), false);
					queryEngine.flushChanges();

					assertThat(agenda.getHighestPriority(), is(Integer.MIN_VALUE));

					assertThat(highPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> highPriorityResultSet.getKey(0));

					assertThat(lowPriorityResultSet.size(), is(0));
					assertThrowsExactly(IndexOutOfBoundsException.class, () -> lowPriorityResultSet.getKey(0));
				}
			}
		}
	}
}
