/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.interpreter.tests.QueryEngineTest;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FilteredView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.term.int_.IntTerms.*;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertNullableResults;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class FunctionalQueryTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private static final Symbol<TruthValue> friend = Symbol.of("friend", 2, TruthValue.class, TruthValue.FALSE);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final FunctionView<Integer> ageView = new FunctionView<>(age);
	private static final AnySymbolView friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

	@QueryEngineTest
	void inputKeyTest(QueryEvaluationHint hint) {
		var query = Query.of("InputKey", Integer.class, (builder, p1, output) -> builder.clause(
				personView.call(p1),
				ageView.call(p1, output)
		));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		ageInterpretation.put(Tuple.of(0), 12);
		ageInterpretation.put(Tuple.of(1), 24);
		ageInterpretation.put(Tuple.of(2), 36);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(12),
				Tuple.of(1), Optional.of(24),
				Tuple.of(2), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void predicateTest(QueryEvaluationHint hint) {
		var subQuery = Query.of("SubQuery", Integer.class, (builder, p1, x) -> builder.clause(
				personView.call(p1),
				ageView.call(p1, x)
		));
		var query = Query.of("Predicate", Integer.class, (builder, p1, output) -> builder.clause(
				personView.call(p1),
				output.assign(subQuery.call(p1))
		));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		ageInterpretation.put(Tuple.of(0), 12);
		ageInterpretation.put(Tuple.of(1), 24);
		ageInterpretation.put(Tuple.of(2), 36);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(12),
				Tuple.of(1), Optional.of(24),
				Tuple.of(2), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void computationTest(QueryEvaluationHint hint) {
		var query = Query.of("Computation", Integer.class, (builder, p1, output) -> builder.clause(() -> {
			var x = Variable.of("x", Integer.class);
			return List.of(
					personView.call(p1),
					ageView.call(p1, x),
					output.assign(mul(x, constant(7)))
			);
		}));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		ageInterpretation.put(Tuple.of(0), 12);
		ageInterpretation.put(Tuple.of(1), 24);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(84),
				Tuple.of(1), Optional.of(168),
				Tuple.of(2), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void inputKeyCountTest(QueryEvaluationHint hint) {
		var query = Query.of("Count", Integer.class, (builder, p1, output) -> builder.clause(
				personView.call(p1),
				output.assign(friendMustView.count(p1, Variable.of()))
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

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(1),
				Tuple.of(1), Optional.of(2),
				Tuple.of(2), Optional.of(0),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void predicateCountTest(QueryEvaluationHint hint) {
		var subQuery = Query.of("SubQuery", (builder, p1, p2) -> builder.clause(
					personView.call(p1),
					personView.call(p2),
					friendMustView.call(p1, p2)
		));
		var query = Query.of("Count", Integer.class, (builder, p1, output) -> builder.clause(
				personView.call(p1),
				output.assign(subQuery.count(p1, Variable.of()))
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

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(1),
				Tuple.of(1), Optional.of(2),
				Tuple.of(2), Optional.of(0),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void inputKeyAggregationTest(QueryEvaluationHint hint) {
		var query = Query.of("Aggregate", Integer.class, (builder, output) -> builder.clause(
				output.assign(ageView.aggregate(INT_SUM, Variable.of()))
		));

		var store = ModelStore.builder()
				.symbols(age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		ageInterpretation.put(Tuple.of(0), 12);
		ageInterpretation.put(Tuple.of(1), 24);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(), 36), queryResultSet);
	}

	@QueryEngineTest
	void predicateAggregationTest(QueryEvaluationHint hint) {
		var subQuery = Query.of("SubQuery", Integer.class, (builder, p1, x) -> builder.clause(
				personView.call(p1),
				ageView.call(p1, x)
		));
		var query = Query.of("Aggregate", Integer.class, (builder, output) -> builder.clause(
				output.assign(subQuery.aggregate(INT_SUM, Variable.of()))
		));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		ageInterpretation.put(Tuple.of(0), 12);
		ageInterpretation.put(Tuple.of(1), 24);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(), 36), queryResultSet);
	}

	@QueryEngineTest
	void extremeValueTest(QueryEvaluationHint hint) {
		var subQuery = Query.of("SubQuery", Integer.class, (builder, p1, x) -> builder.clause(
				personView.call(p1),
				x.assign(friendMustView.count(p1, Variable.of()))
		));
		var minQuery = Query.of("Min", Integer.class, (builder, output) -> builder.clause(
				output.assign(subQuery.aggregate(INT_MIN, Variable.of()))
		));
		var maxQuery = Query.of("Max", Integer.class, (builder, output) -> builder.clause(
				output.assign(subQuery.aggregate(INT_MAX, Variable.of()))
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(minQuery, maxQuery))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var minResultSet = queryEngine.getResultSet(minQuery);
		var maxResultSet = queryEngine.getResultSet(maxQuery);

		assertResults(Map.of(Tuple.of(), Integer.MAX_VALUE), minResultSet);
		assertResults(Map.of(Tuple.of(), Integer.MIN_VALUE), maxResultSet);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(), 0), minResultSet);
		assertResults(Map.of(Tuple.of(), 2), maxResultSet);

		friendInterpretation.put(Tuple.of(2, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(2, 1), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(), 1), minResultSet);
		assertResults(Map.of(Tuple.of(), 2), maxResultSet);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.FALSE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.FALSE);
		friendInterpretation.put(Tuple.of(2, 0), TruthValue.FALSE);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(), 0), minResultSet);
		assertResults(Map.of(Tuple.of(), 1), maxResultSet);
	}

	@QueryEngineTest
	void invalidComputationTest(QueryEvaluationHint hint) {
		var query = Query.of("InvalidComputation", Integer.class,
				(builder, p1, output) -> builder.clause(Integer.class, (x) -> List.of(
						personView.call(p1),
						ageView.call(p1, x),
						output.assign(div(constant(120), x))
				)));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		ageInterpretation.put(Tuple.of(0), 0);
		ageInterpretation.put(Tuple.of(1), 30);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.empty(),
				Tuple.of(1), Optional.of(4),
				Tuple.of(2), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void invalidAssumeTest(QueryEvaluationHint hint) {
		var query = Query.of("InvalidAssume", (builder, p1) -> builder.clause(Integer.class, (x) -> List.of(
				personView.call(p1),
				ageView.call(p1, x),
				check(lessEq(div(constant(120), x), constant(5)))
		)));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		ageInterpretation.put(Tuple.of(0), 0);
		ageInterpretation.put(Tuple.of(1), 30);
		ageInterpretation.put(Tuple.of(2), 20);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), queryResultSet);
	}

	@QueryEngineTest
	void multipleAssignmentTest(QueryEvaluationHint hint) {
		var query = Query.of("MultipleAssignment", Integer.class, (builder, p1, p2, output) -> builder
				.clause(Integer.class, Integer.class, (x1, x2) -> List.of(
						ageView.call(p1, x1),
						ageView.call(p2, x2),
						output.assign(mul(x1, constant(2))),
						output.assign(mul(x2, constant(3)))
				)));

		var store = ModelStore.builder()
				.symbols(age)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		ageInterpretation.put(Tuple.of(0), 3);
		ageInterpretation.put(Tuple.of(1), 2);
		ageInterpretation.put(Tuple.of(2), 15);
		ageInterpretation.put(Tuple.of(3), 10);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0, 1), Optional.of(6),
				Tuple.of(1, 0), Optional.empty(),
				Tuple.of(2, 3), Optional.of(30),
				Tuple.of(3, 2), Optional.empty()
		), queryResultSet);
	}

	@QueryEngineTest
	void notFunctionalTest(QueryEvaluationHint hint) {
		var query = Query.of("NotFunctional", Integer.class, (builder, p1, output) -> builder.clause((p2) -> List.of(
				personView.call(p1),
				friendMustView.call(p1, p2),
				ageView.call(p2, output)
		)));

		var store = ModelStore.builder()
				.symbols(person, age, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		ageInterpretation.put(Tuple.of(0), 24);
		ageInterpretation.put(Tuple.of(1), 30);
		ageInterpretation.put(Tuple.of(2), 36);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		var invalidTuple = Tuple.of(1);
		var cursor = queryResultSet.getAll();
		assertAll(
				() -> assertThat("value for key 0", queryResultSet.get(Tuple.of(0)), is(30)),
				() -> assertThrows(IllegalStateException.class, () -> queryResultSet.get(invalidTuple),
						"multiple values for key 1"),
				() -> assertThat("value for key 2", queryResultSet.get(Tuple.of(2)), is(nullValue())),
				() -> assertThat("value for key 3", queryResultSet.get(Tuple.of(3)), is(nullValue()))
		);
		if (hint.getQueryBackendRequirementType() != QueryEvaluationHint.BackendRequirement.DEFAULT_SEARCH) {
			// Local search doesn't support throwing an error on multiple function return values.
			assertThat("results size", queryResultSet.size(), is(2));
			assertThrows(IllegalStateException.class, () -> enumerateValues(cursor), "move cursor");
		}
	}

	@QueryEngineTest
	void multipleFunctionalQueriesTest(QueryEvaluationHint hint) {
		var subQuery1 = Query.of("SubQuery1", Integer.class, (builder, p1, output) -> builder.clause(
				personView.call(p1),
				ageView.call(p1, output)
		));
		var subQuery2 = Query.of("SubQuery2", Integer.class, (builder, p1, output) -> builder.clause(
				personView.call(p1),
				output.assign(friendMustView.count(p1, Variable.of()))
		));
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, (v1, v2) -> List.of(
				v1.assign(subQuery1.call(p1)),
				v2.assign(subQuery2.call(p1)),
				output.assign(add(v1, v2))
		)));

		var store = ModelStore.builder()
				.symbols(person, age, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		ageInterpretation.put(Tuple.of(0), 24);
		ageInterpretation.put(Tuple.of(1), 30);
		ageInterpretation.put(Tuple.of(2), 36);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(25),
				Tuple.of(1), Optional.of(32),
				Tuple.of(2), Optional.of(36),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	private static void enumerateValues(Cursor<?, ?> cursor) {
		//noinspection StatementWithEmptyBody
		while (cursor.move()) {
			// Nothing do, just let the cursor move through the result set.
		}
	}
}
