/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.interpreter.tests.QueryEngineTest;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.INT_SUM;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertNullableResults;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class DiagonalQueryTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private static final Symbol<Boolean> symbol = Symbol.of("symbol", 4);
	private static final Symbol<Integer> intSymbol = Symbol.of("intSymbol", 4, Integer.class);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final AnySymbolView friendView = new KeyOnlyView<>(friend);
	private static final AnySymbolView symbolView = new KeyOnlyView<>(symbol);
	private static final FunctionView<Integer> intSymbolView = new FunctionView<>(intSymbol);

	@QueryEngineTest
	void inputKeyNegationTest(QueryEvaluationHint hint) {
		var query = Query.of("Diagonal", (builder, p1) -> builder.clause(p2 -> List.of(
				personView.call(p1),
				not(symbolView.call(p1, p1, p2, p2))
		)));

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0, 1, 1), true);
		symbolInterpretation.put(Tuple.of(0, 0, 1, 2), true);
		symbolInterpretation.put(Tuple.of(1, 1, 0, 1), true);
		symbolInterpretation.put(Tuple.of(1, 2, 1, 1), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), true,
				Tuple.of(2), true,
				Tuple.of(3), false
		), queryResultSet);
	}

	@QueryEngineTest
	void subQueryNegationTest(QueryEvaluationHint hint) {
		var subQuery = Query.of("SubQuery", (builder, p1, p2, p3, p4) -> builder
				.clause(
						personView.call(p1),
						symbolView.call(p1, p2, p3, p4)
				)
				.clause(
						personView.call(p2),
						symbolView.call(p1, p2, p3, p4)
				));
		var query = Query.of("Diagonal", (builder, p1) -> builder.clause(p2 -> List.of(
				personView.call(p1),
				not(subQuery.call(p1, p1, p2, p2))
		)));

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();

		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0, 1, 1), true);
		symbolInterpretation.put(Tuple.of(0, 0, 1, 2), true);
		symbolInterpretation.put(Tuple.of(1, 1, 0, 1), true);
		symbolInterpretation.put(Tuple.of(1, 2, 1, 1), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), true,
				Tuple.of(2), true,
				Tuple.of(3), false
		), queryResultSet);
	}

	@QueryEngineTest
	void inputKeyCountTest(QueryEvaluationHint hint) {
		var query = Query.of("Diagonal", Integer.class, (builder, p1, output) -> builder.clause(p2 -> List.of(
				personView.call(p1),
				output.assign(symbolView.count(p1, p1, p2, p2))
		)));

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0, 1, 1), true);
		symbolInterpretation.put(Tuple.of(0, 0, 2, 2), true);
		symbolInterpretation.put(Tuple.of(0, 0, 1, 2), true);
		symbolInterpretation.put(Tuple.of(1, 1, 0, 1), true);
		symbolInterpretation.put(Tuple.of(1, 2, 1, 1), true);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(2),
				Tuple.of(1), Optional.of(0),
				Tuple.of(2), Optional.of(0),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void subQueryCountTest(QueryEvaluationHint hint) {
		var subQuery = Query.of("SubQuery", (builder, p1, p2, p3, p4) -> builder.clause(
						personView.call(p1),
						symbolView.call(p1, p2, p3, p4)
				)
				.clause(
						personView.call(p2),
						symbolView.call(p1, p2, p3, p4)
				));
		var query = Query.of("Diagonal", Integer.class, (builder, p1, output) -> builder.clause(p2 -> List.of(
				personView.call(p1),
				output.assign(subQuery.count(p1, p1, p2, p2))
		)));

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0, 1, 1), true);
		symbolInterpretation.put(Tuple.of(0, 0, 2, 2), true);
		symbolInterpretation.put(Tuple.of(0, 0, 1, 2), true);
		symbolInterpretation.put(Tuple.of(1, 1, 0, 1), true);
		symbolInterpretation.put(Tuple.of(1, 2, 1, 1), true);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(2),
				Tuple.of(1), Optional.of(0),
				Tuple.of(2), Optional.of(0),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void inputKeyAggregationTest(QueryEvaluationHint hint) {
		var query = Query.of("Diagonal", Integer.class, (builder, p1, output) -> builder
				.clause((p2) -> List.of(
						personView.call(p1),
						output.assign(intSymbolView.aggregate(INT_SUM, p1, p1, p2, p2))
				)));

		var store = ModelStore.builder()
				.symbols(person, intSymbol)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var intSymbolInterpretation = model.getInterpretation(intSymbol);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		intSymbolInterpretation.put(Tuple.of(0, 0, 1, 1), 1);
		intSymbolInterpretation.put(Tuple.of(0, 0, 2, 2), 2);
		intSymbolInterpretation.put(Tuple.of(0, 0, 1, 2), 10);
		intSymbolInterpretation.put(Tuple.of(1, 1, 0, 1), 11);
		intSymbolInterpretation.put(Tuple.of(1, 2, 1, 1), 12);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(3),
				Tuple.of(1), Optional.of(0),
				Tuple.of(2), Optional.of(0),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void subQueryAggregationTest(QueryEvaluationHint hint) {
		var subQuery = Dnf.of("SubQuery", builder -> {
			var p1 = builder.parameter("p1");
			var p2 = builder.parameter("p2");
			var p3 = builder.parameter("p3");
			var p4 = builder.parameter("p4");
			var x = builder.parameter("x", Integer.class);
			var y = builder.parameter("y", Integer.class);
			builder.clause(
					personView.call(p1),
					intSymbolView.call(p1, p2, p3, p4, x),
					y.assign(x)
			);
			builder.clause(
					personView.call(p2),
					intSymbolView.call(p1, p2, p3, p4, x),
					y.assign(x)
			);
		});
		var query = Query.of("Diagonal", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, (p2, y, z) -> List.of(
						personView.call(p1),
						output.assign(subQuery.aggregateBy(y, INT_SUM, p1, p1, p2, p2, y, z))
				)));

		var store = ModelStore.builder()
				.symbols(person, intSymbol)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var intSymbolInterpretation = model.getInterpretation(intSymbol);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		intSymbolInterpretation.put(Tuple.of(0, 0, 1, 1), 1);
		intSymbolInterpretation.put(Tuple.of(0, 0, 2, 2), 2);
		intSymbolInterpretation.put(Tuple.of(0, 0, 1, 2), 10);
		intSymbolInterpretation.put(Tuple.of(1, 1, 0, 1), 11);
		intSymbolInterpretation.put(Tuple.of(1, 2, 1, 1), 12);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(3),
				Tuple.of(1), Optional.of(0),
				Tuple.of(2), Optional.of(0),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void inputKeyTransitiveTest(QueryEvaluationHint hint) {
		var query = Query.of("Diagonal", (builder, p1) -> builder.clause(
				personView.call(p1),
				friendView.callTransitive(p1, p1)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 0), true);
		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), false,
				Tuple.of(2), false,
				Tuple.of(3), false
		), queryResultSet);
	}

	@QueryEngineTest
	void subQueryTransitiveTest(QueryEvaluationHint hint) {
		var subQuery = Query.of("SubQuery", (builder, p1, p2) -> builder
				.clause(
						personView.call(p1),
						friendView.call(p1, p2)
				)
				.clause(
						personView.call(p2),
						friendView.call(p1, p2)
				));
		var query = Query.of("Diagonal", (builder, p1) -> builder.clause(
				personView.call(p1),
				subQuery.callTransitive(p1, p1)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 0), true);
		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), false,
				Tuple.of(2), false,
				Tuple.of(3), false
		), queryResultSet);
	}
}
