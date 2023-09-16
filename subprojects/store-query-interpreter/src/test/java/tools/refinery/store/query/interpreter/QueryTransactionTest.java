/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FilteredView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertNullableResults;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class QueryTransactionTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final AnySymbolView ageView = new FunctionView<>(age);
	private static final RelationalQuery predicate = Query.of("TypeConstraint", (builder, p1) ->
			builder.clause(personView.call(p1)));

	@Test
	void flushTest() {
		var store = ModelStore.builder()
				.symbols(person)
				.with(QueryInterpreterAdapter.builder()
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), false,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), false,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
		assertTrue(queryEngine.hasPendingChanges());

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());

		personInterpretation.put(Tuple.of(1), false);
		personInterpretation.put(Tuple.of(2), true);

		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
		assertTrue(queryEngine.hasPendingChanges());

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), false,
				Tuple.of(2), true,
				Tuple.of(3), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());
	}

	@Test
	void localSearchTest() {
		var store = ModelStore.builder()
				.symbols(person)
				.with(QueryInterpreterAdapter.builder()
					.defaultHint(new QueryEvaluationHint(null, QueryEvaluationHint.BackendRequirement.DEFAULT_SEARCH))
					.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), false,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());

		personInterpretation.put(Tuple.of(1), false);
		personInterpretation.put(Tuple.of(2), true);

		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), false,
				Tuple.of(2), true,
				Tuple.of(3), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());
	}

	@Test
	void unrelatedChangesTest() {
		var asset = Symbol.of("Asset", 1);

		var store = ModelStore.builder()
				.symbols(person, asset)
				.with(QueryInterpreterAdapter.builder()
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var assetInterpretation = model.getInterpretation(asset);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		assertFalse(queryEngine.hasPendingChanges());

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		assetInterpretation.put(Tuple.of(1), true);
		assetInterpretation.put(Tuple.of(2), true);

		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), false,
				Tuple.of(2), false,
				Tuple.of(3), false,
				Tuple.of(4), false
		), predicateResultSet);
		assertTrue(queryEngine.hasPendingChanges());

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false,
				Tuple.of(4), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());

		assetInterpretation.put(Tuple.of(3), true);
		assertFalse(queryEngine.hasPendingChanges());

		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false,
				Tuple.of(4), false
		), predicateResultSet);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false,
				Tuple.of(4), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());
	}

	@Test
	void tupleChangingChangeTest() {
		var query = Query.of("TypeConstraint", Integer.class, (builder, p1, output) -> builder.clause(
				personView.call(p1),
				ageView.call(p1, output)
		));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);

		ageInterpretation.put(Tuple.of(0), 24);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(0), 24), queryResultSet);

		ageInterpretation.put(Tuple.of(0), 25);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(0), 25), queryResultSet);

		ageInterpretation.put(Tuple.of(0), null);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(Tuple.of(0), Optional.empty()), queryResultSet);
	}

	@Test
	void tuplePreservingUnchangedTest() {
		var adultView = new FilteredView<>(age, "adult", n -> n != null && n >= 18);

		var query = Query.of("TypeConstraint", (builder, p1) -> builder.clause(
				personView.call(p1),
				adultView.call(p1)
		));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);

		ageInterpretation.put(Tuple.of(0), 24);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(0), true), queryResultSet);

		ageInterpretation.put(Tuple.of(0), 25);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(0), true), queryResultSet);

		ageInterpretation.put(Tuple.of(0), 17);

		queryEngine.flushChanges();
		assertResults(Map.of(Tuple.of(0), false), queryResultSet);
	}

	@Test
	void commitAfterFlushTest() {
		var store = ModelStore.builder()
				.symbols(person)
				.with(QueryInterpreterAdapter.builder()
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);

		var state1 = model.commit();

		personInterpretation.put(Tuple.of(1), false);
		personInterpretation.put(Tuple.of(2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), false,
				Tuple.of(2), true,
				Tuple.of(3), false
		), predicateResultSet);

		model.restore(state1);

		assertFalse(queryEngine.hasPendingChanges());
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
	}

	@Test
	void commitWithoutFlushTest() {
		var store = ModelStore.builder()
				.symbols(person)
				.with(QueryInterpreterAdapter.builder()
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		assertResults(Map.of(), predicateResultSet);
		assertTrue(queryEngine.hasPendingChanges());

		var state1 = model.commit();

		personInterpretation.put(Tuple.of(1), false);
		personInterpretation.put(Tuple.of(2), true);

		assertResults(Map.of(), predicateResultSet);
		assertTrue(queryEngine.hasPendingChanges());

		model.restore(state1);

		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
		assertFalse(queryEngine.hasPendingChanges());
	}
}
