/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQuery;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.viatra.tests.QueryEngineTest;
import tools.refinery.store.query.view.FilteredView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.literal.Literals.assume;
import static tools.refinery.store.query.term.int_.IntTerms.*;
import static tools.refinery.store.query.viatra.tests.QueryAssertions.assertNullableResults;
import static tools.refinery.store.query.viatra.tests.QueryAssertions.assertResults;

class FunctionalQueryTest {
	@QueryEngineTest
	void inputKeyTest(QueryEvaluationHint hint) {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var query = Query.builder("InputKey")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						ageView.call(p1, x)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, x)
				.clause(
						personView.call(p1),
						ageView.call(p1, x)
				)
				.build();
		var query = Query.builder("Predicate")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						subQuery.call(p1, x)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var y = Variable.of("y", Integer.class);
		var query = Query.builder("Computation")
				.parameter(p1)
				.output(y)
				.clause(
						personView.call(p1),
						ageView.call(p1, x),
						y.assign(mul(x, constant(7)))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var x = Variable.of("x", Integer.class);
		var query = Query.builder("Count")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						x.assign(friendMustView.count(p1, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var x = Variable.of("x", Integer.class);
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();
		var query = Query.builder("Count")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						x.assign(subQuery.count(p1, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var age = new Symbol<>("age", 1, Integer.class, null);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var y = Variable.of("y", Integer.class);
		var query = Query.builder("Aggregate")
				.output(x)
				.clause(
						x.assign(ageView.aggregate(y, INT_SUM, p1, y))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(age)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var queryResultSet = queryEngine.getResultSet(query);

		ageInterpretation.put(Tuple.of(0), 12);
		ageInterpretation.put(Tuple.of(1), 24);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(), 36), queryResultSet);
	}

	@QueryEngineTest
	void predicateAggregationTest(QueryEvaluationHint hint) {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var y = Variable.of("y", Integer.class);
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, x)
				.clause(
						personView.call(p1),
						ageView.call(p1, x)
				)
				.build();
		var query = Query.builder("Aggregate")
				.output(x)
				.clause(
						x.assign(subQuery.aggregate(y, INT_SUM, p1, y))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var x = Variable.of("x", Integer.class);
		var y = Variable.of("y", Integer.class);
		var subQuery = Dnf.builder("SubQuery")
				.parameters(p1, x)
				.clause(
						personView.call(p1),
						x.assign(friendMustView.count(p1, p2))
				)
				.build();
		var minQuery = Query.builder("Min")
				.output(x)
				.clause(
						x.assign(subQuery.aggregate(y, INT_MIN, p1, y))
				)
				.build();
		var maxQuery = Query.builder("Max")
				.output(x)
				.clause(
						x.assign(subQuery.aggregate(y, INT_MAX, p1, y))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(minQuery, maxQuery)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var y = Variable.of("y", Integer.class);
		var query = Query.builder("InvalidComputation")
				.parameter(p1)
				.output(y)
				.clause(
						personView.call(p1),
						ageView.call(p1, x),
						y.assign(div(constant(120), x))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var query = Query.builder("InvalidComputation")
				.parameter(p1)
				.clause(
						personView.call(p1),
						ageView.call(p1, x),
						assume(lessEq(div(constant(120), x), constant(5)))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
	void notFunctionalTest(QueryEvaluationHint hint) {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var x = Variable.of("x", Integer.class);
		var query = Query.builder("NotFunctional")
				.parameter(p1)
				.output(x)
				.clause(
						personView.call(p1),
						friendMustView.call(p1, p2),
						ageView.call(p2, x)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, age, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.query(query)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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

	private static void enumerateValues(Cursor<?, ?> cursor) {
		//noinspection StatementWithEmptyBody
		while (cursor.move()) {
			// Nothing do, just let the cursor move through the result set.
		}
	}
}
