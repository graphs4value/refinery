/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.viatra.tests.QueryEngineTest;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Map;
import java.util.Optional;

import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.INT_SUM;
import static tools.refinery.store.query.viatra.tests.QueryAssertions.assertNullableResults;
import static tools.refinery.store.query.viatra.tests.QueryAssertions.assertResults;

class DiagonalQueryTest {
	@QueryEngineTest
	void inputKeyNegationTest(QueryEvaluationHint hint) {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 4, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new KeyOnlyView<>(symbol);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.clause(
						personView.call(p1),
						not(symbolView.call(p1, p1, p2, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 4, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new KeyOnlyView<>(symbol);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var p3 = Variable.of("p3");
		var p4 = Variable.of("p4");
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, p2, p3, p4)
				.clause(
						personView.call(p1),
						symbolView.call(p1, p2, p3, p4)
				)
				.clause(
						personView.call(p2),
						symbolView.call(p1, p2, p3, p4)
				)
				.build();
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.clause(
						personView.call(p1),
						not(subQuery.call(p1, p1, p2, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 4, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new KeyOnlyView<>(symbol);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var x = Variable.of("x", Integer.class);
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						x.assign(symbolView.count(p1, p1, p2, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 4, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new KeyOnlyView<>(symbol);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var p3 = Variable.of("p3");
		var p4 = Variable.of("p4");
		var x = Variable.of("x", Integer.class);
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, p2, p3, p4)
				.clause(
						personView.call(p1),
						symbolView.call(p1, p2, p3, p4)
				)
				.clause(
						personView.call(p2),
						symbolView.call(p1, p2, p3, p4)
				)
				.build();
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						x.assign(subQuery.count(p1, p1, p2, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 4, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new FunctionView<>(symbol);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var x = Variable.of("x", Integer.class);
		var y = Variable.of("y", Integer.class);
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						x.assign(symbolView.aggregate(y, INT_SUM, p1, p1, p2, p2, y))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0, 1, 1), 1);
		symbolInterpretation.put(Tuple.of(0, 0, 2, 2), 2);
		symbolInterpretation.put(Tuple.of(0, 0, 1, 2), 10);
		symbolInterpretation.put(Tuple.of(1, 1, 0, 1), 11);
		symbolInterpretation.put(Tuple.of(1, 2, 1, 1), 12);

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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 4, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new FunctionView<>(symbol);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var p3 = Variable.of("p3");
		var p4 = Variable.of("p4");
		var x = Variable.of("x", Integer.class);
		var y = Variable.of("y", Integer.class);
		var z = Variable.of("z", Integer.class);
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, p2, p3, p4, x, y)
				.clause(
						personView.call(p1),
						symbolView.call(p1, p2, p3, p4, x),
						y.assign(x)
				)
				.clause(
						personView.call(p2),
						symbolView.call(p1, p2, p3, p4, x),
						y.assign(x)
				)
				.build();
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						x.assign(subQuery.aggregate(z, INT_SUM, p1, p1, p2, p2, z, z))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0, 1, 1), 1);
		symbolInterpretation.put(Tuple.of(0, 0, 2, 2), 2);
		symbolInterpretation.put(Tuple.of(0, 0, 1, 2), 10);
		symbolInterpretation.put(Tuple.of(1, 1, 0, 1), 11);
		symbolInterpretation.put(Tuple.of(1, 2, 1, 1), 12);

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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 2, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new KeyOnlyView<>(symbol);

		var p1 = Variable.of("p1");
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.clause(
						personView.call(p1),
						symbolView.callTransitive(p1, p1)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0), true);
		symbolInterpretation.put(Tuple.of(0, 1), true);
		symbolInterpretation.put(Tuple.of(1, 2), true);

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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var symbol = new Symbol<>("symbol", 2, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);
		var symbolView = new KeyOnlyView<>(symbol);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						symbolView.call(p1, p2)
				)
				.clause(
						personView.call(p2),
						symbolView.call(p1, p2)
				)
				.build();
		var query = Query.builder("Diagonal")
				.parameter(p1)
				.clause(
						personView.call(p1),
						subQuery.callTransitive(p1, p1)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, symbol)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var symbolInterpretation = model.getInterpretation(symbol);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		symbolInterpretation.put(Tuple.of(0, 0), true);
		symbolInterpretation.put(Tuple.of(0, 1), true);
		symbolInterpretation.put(Tuple.of(1, 2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), false,
				Tuple.of(2), false,
				Tuple.of(3), false
		), queryResultSet);
	}
}
