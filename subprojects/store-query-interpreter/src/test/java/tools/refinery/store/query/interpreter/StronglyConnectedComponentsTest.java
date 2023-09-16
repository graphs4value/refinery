/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.literal.Connectivity;
import tools.refinery.store.query.literal.RepresentativeElectionLiteral;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class StronglyConnectedComponentsTest {
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);

	@Test
	void symbolViewTest() {
		var query = Query.of("SymbolViewRepresentative", (builder, p1, p2) -> builder
				.clause(v1 -> List.of(
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p1, v1),
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p2, v1)
				)));

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
		friendInterpretation.put(Tuple.of(1, 0), true);
		friendInterpretation.put(Tuple.of(1, 2), true);
		queryEngine.flushChanges();

		assertResults(Map.of(
				Tuple.of(0, 0), true,
				Tuple.of(0, 1), true,
				Tuple.of(1, 0), true,
				Tuple.of(1, 1), true,
				Tuple.of(2, 2), true
		), resultSet);
	}

	@Test
	void symbolViewInsertTest() {
		var query = Query.of("SymbolViewRepresentative", (builder, p1, p2) -> builder
				.clause(v1 -> List.of(
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p1, v1),
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p2, v1)
				)));

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
		friendInterpretation.put(Tuple.of(1, 0), true);
		friendInterpretation.put(Tuple.of(1, 2), true);
		queryEngine.flushChanges();

		friendInterpretation.put(Tuple.of(2, 0), true);
		friendInterpretation.put(Tuple.of(0, 3), true);
		queryEngine.flushChanges();

		assertResults(Map.of(
				Tuple.of(0, 0), true,
				Tuple.of(0, 1), true,
				Tuple.of(0, 2), true,
				Tuple.of(1, 1), true,
				Tuple.of(1, 0), true,
				Tuple.of(1, 2), true,
				Tuple.of(2, 0), true,
				Tuple.of(2, 1), true,
				Tuple.of(2, 2), true,
				Tuple.of(3, 3), true
		), resultSet);
	}

	@Test
	void symbolViewDeleteTest() {
		var query = Query.of("SymbolViewRepresentative", (builder, p1, p2) -> builder
				.clause(v1 -> List.of(
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p1, v1),
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p2, v1)
				)));

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
		friendInterpretation.put(Tuple.of(1, 0), true);
		friendInterpretation.put(Tuple.of(1, 2), true);
		queryEngine.flushChanges();

		friendInterpretation.put(Tuple.of(1, 0), false);
		friendInterpretation.put(Tuple.of(1, 2), false);
		queryEngine.flushChanges();

		assertResults(Map.of(
				Tuple.of(0, 0), true,
				Tuple.of(1, 1), true
		), resultSet);
	}

	@Test
	void diagonalSymbolViewTest() {
		var person = Symbol.of("Person", 1);
		var personView = new KeyOnlyView<>(person);

		var query = Query.of("SymbolViewRepresentative", (builder, p1) -> builder
				.clause(
						personView.call(p1),
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p1, p1)
				));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var resultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 0), true);
		friendInterpretation.put(Tuple.of(1, 2), true);
		queryEngine.flushChanges();

		assertThat(resultSet.size(), is(2));
		assertThat(resultSet.get(Tuple.of(2)), is(true));
	}

	@Test
	void diagonalDnfTest() {
		var person = Symbol.of("Person", 1);
		var personView = new KeyOnlyView<>(person);

		var subQuery = Query.of("SubQuery", (builder, p1, p2) -> builder
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendView.call(p1, p2)
				))
				.getDnf();
		var query = Query.of("SymbolViewRepresentative", (builder, p1) -> builder
				.clause(
						personView.call(p1),
						new RepresentativeElectionLiteral(Connectivity.STRONG, subQuery, p1, p1)
				));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var resultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 0), true);
		friendInterpretation.put(Tuple.of(1, 2), true);
		queryEngine.flushChanges();

		assertThat(resultSet.size(), is(2));
		assertThat(resultSet.get(Tuple.of(2)), is(true));
	}

	@Test
	void loopTest() {
		var query = Query.of("SymbolViewRepresentative", (builder, p1, p2) -> builder
				.clause(v1 -> List.of(
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p1, v1),
						new RepresentativeElectionLiteral(Connectivity.STRONG, friendView, p2, v1)
				)));

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
		friendInterpretation.put(Tuple.of(2, 3), true);
		friendInterpretation.put(Tuple.of(3, 0), true);
		friendInterpretation.put(Tuple.of(3, 4), true);
		queryEngine.flushChanges();

		assertThat(resultSet.get(Tuple.of(0, 1)), is(true));
		assertThat(resultSet.get(Tuple.of(1, 2)), is(true));
		assertThat(resultSet.get(Tuple.of(2, 3)), is(true));
		assertThat(resultSet.get(Tuple.of(3, 0)), is(true));
		assertThat(resultSet.get(Tuple.of(3, 4)), is(false));

		friendInterpretation.put(Tuple.of(2, 3), false);
		queryEngine.flushChanges();

		assertThat(resultSet.get(Tuple.of(0, 1)), is(false));
		assertThat(resultSet.get(Tuple.of(0, 2)), is(false));
		assertThat(resultSet.get(Tuple.of(0, 3)), is(false));
		assertThat(resultSet.get(Tuple.of(1, 2)), is(false));
		assertThat(resultSet.get(Tuple.of(2, 3)), is(false));
		assertThat(resultSet.get(Tuple.of(3, 0)), is(false));
		assertThat(resultSet.get(Tuple.of(3, 4)), is(false));
	}
}
