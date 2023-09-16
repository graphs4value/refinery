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
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.ParameterDirection;
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

import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.greaterEq;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class QueryTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<TruthValue> friend = Symbol.of("friend", 2, TruthValue.class, TruthValue.FALSE);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final AnySymbolView friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

	@QueryEngineTest
	void typeConstraintTest(QueryEvaluationHint hint) {
		var asset = Symbol.of("Asset", 1);

		var predicate = Query.of("TypeConstraint", (builder, p1) -> builder.clause(personView.call(p1)));

		var store = ModelStore.builder()
				.symbols(person, asset)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var assetInterpretation = model.getInterpretation(asset);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		assetInterpretation.put(Tuple.of(1), true);
		assetInterpretation.put(Tuple.of(2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void relationConstraintTest(QueryEvaluationHint hint) {
		var predicate = Query.of("RelationConstraint", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p2)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 3), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1), true,
				Tuple.of(1, 0), true,
				Tuple.of(1, 2), true,
				Tuple.of(2, 1), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void isConstantTest(QueryEvaluationHint hint) {
		var predicate = Query.of("RelationConstraint", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				p1.isConstant(1),
				friendMustView.call(p1, p2)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1), false,
				Tuple.of(1, 0), true,
				Tuple.of(1, 2), true,
				Tuple.of(2, 1), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void existTest(QueryEvaluationHint hint) {
		var predicate = Query.of("Exists", (builder, p1) -> builder.clause((p2) -> List.of(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p2)
		)));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(3, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void orTest(QueryEvaluationHint hint) {
		var animal = Symbol.of("Animal", 1);
		var animalView = new KeyOnlyView<>(animal);

		var predicate = Query.of("Or", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p2)
		).clause(
				animalView.call(p1),
				animalView.call(p2),
				friendMustView.call(p1, p2)
		));

		var store = ModelStore.builder()
				.symbols(person, animal, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var animalInterpretation = model.getInterpretation(animal);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		animalInterpretation.put(Tuple.of(2), true);
		animalInterpretation.put(Tuple.of(3), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(0, 2), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(2, 3), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(3, 0), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1), true,
				Tuple.of(0, 2), false,
				Tuple.of(2, 3), true,
				Tuple.of(3, 0), false,
				Tuple.of(3, 2), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void equalityTest(QueryEvaluationHint hint) {
		var predicate = Query.of("Equality", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				p1.isEquivalent(p2)
		));

		var store = ModelStore.builder()
				.symbols(person)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 0), true,
				Tuple.of(1, 1), true,
				Tuple.of(2, 2), true,
				Tuple.of(0, 1), false,
				Tuple.of(3, 3), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void inequalityTest(QueryEvaluationHint hint) {
		var predicate = Query.of("Inequality", (builder, p1, p2, p3) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p3),
				friendMustView.call(p2, p3),
				p1.notEquivalent(p2)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 2), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1, 2), true,
				Tuple.of(1, 0, 2), true,
				Tuple.of(0, 0, 2), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void patternCallTest(QueryEvaluationHint hint) {
		var friendPredicate = Query.of("Friend", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p2)
		));
		var predicate = Query.of("PositivePatternCall", (builder, p3, p4) -> builder.clause(
				personView.call(p3),
				personView.call(p4),
				friendPredicate.call(p3, p4)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1), true,
				Tuple.of(1, 0), true,
				Tuple.of(1, 2), true,
				Tuple.of(2, 1), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void patternCallInputArgumentTest(QueryEvaluationHint hint) {
		var friendPredicate = Dnf.of("Friend", builder -> {
			var p1 = builder.parameter("p1", ParameterDirection.IN);
			var p2 = builder.parameter("p2", ParameterDirection.IN);
			builder.clause(
					personView.call(p1),
					personView.call(p2),
					friendMustView.call(p1, p2)
			);
		});
		var predicate = Query.of("PositivePatternCall", (builder, p3, p4) -> builder.clause(
				personView.call(p3),
				personView.call(p4),
				friendPredicate.call(p3, p4)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1), true,
				Tuple.of(1, 0), true,
				Tuple.of(1, 2), true,
				Tuple.of(2, 1), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void negativeRelationViewTest(QueryEvaluationHint hint) {
		var predicate = Query.of("NegativePatternCall", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				not(friendMustView.call(p1, p2))
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 0), true,
				Tuple.of(0, 2), true,
				Tuple.of(1, 1), true,
				Tuple.of(2, 0), true,
				Tuple.of(2, 1), true,
				Tuple.of(2, 2), true,
				Tuple.of(0, 1), false,
				Tuple.of(1, 0), false,
				Tuple.of(1, 2), false,
				Tuple.of(0, 3), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void negativePatternCallTest(QueryEvaluationHint hint) {
		var friendPredicate = Query.of("Friend", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p2)
		));
		var predicate = Query.of("NegativePatternCall", (builder, p3, p4) -> builder.clause(
				personView.call(p3),
				personView.call(p4),
				not(friendPredicate.call(p3, p4))
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 0), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 0), true,
				Tuple.of(0, 2), true,
				Tuple.of(1, 1), true,
				Tuple.of(2, 0), true,
				Tuple.of(2, 1), true,
				Tuple.of(2, 2), true,
				Tuple.of(0, 1), false,
				Tuple.of(1, 0), false,
				Tuple.of(1, 2), false,
				Tuple.of(0, 3), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void negativeRelationViewWithQuantificationTest(QueryEvaluationHint hint) {
		var predicate = Query.of("Negative", (builder, p1) -> builder.clause(
				personView.call(p1),
				not(friendMustView.call(p1, Variable.of()))
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(0, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), true,
				Tuple.of(2), true,
				Tuple.of(3), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void negativeWithQuantificationTest(QueryEvaluationHint hint) {
		var called = Query.of("Called", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p2)
		));
		var predicate = Query.of("Negative", (builder, p1) -> builder.clause(
				personView.call(p1),
				not(called.call(p1, Variable.of()))
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(0, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), true,
				Tuple.of(2), true,
				Tuple.of(3), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void transitiveRelationViewTest(QueryEvaluationHint hint) {
		var predicate = Query.of("Transitive", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.callTransitive(p1, p2)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 0), false,
				Tuple.of(0, 1), true,
				Tuple.of(0, 2), true,
				Tuple.of(1, 0), false,
				Tuple.of(1, 1), false,
				Tuple.of(1, 2), true,
				Tuple.of(2, 0), false,
				Tuple.of(2, 1), false,
				Tuple.of(2, 2), false,
				Tuple.of(2, 3), false
		), predicateResultSet);
	}

	@QueryEngineTest
	void transitivePatternCallTest(QueryEvaluationHint hint) {
		var called = Query.of("Called", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				friendMustView.call(p1, p2)
		));
		var predicate = Query.of("Transitive", (builder, p1, p2) -> builder.clause(
				personView.call(p1),
				personView.call(p2),
				called.callTransitive(p1, p2)
		));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.defaultHint(hint)
						.queries(predicate))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		friendInterpretation.put(Tuple.of(0, 1), TruthValue.TRUE);
		friendInterpretation.put(Tuple.of(1, 2), TruthValue.TRUE);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 0), false,
				Tuple.of(0, 1), true,
				Tuple.of(0, 2), true,
				Tuple.of(1, 0), false,
				Tuple.of(1, 1), false,
				Tuple.of(1, 2), true,
				Tuple.of(2, 0), false,
				Tuple.of(2, 1), false,
				Tuple.of(2, 2), false,
				Tuple.of(2, 3), false
		), predicateResultSet);
	}

	@Test
	void filteredIntegerViewTest() {
		var distance = Symbol.of("distance", 2, Integer.class);
		var nearView = new FilteredView<>(distance, value -> value < 2);
		var farView = new FilteredView<>(distance, value -> value >= 5);
		var dangerQuery = Query.of("danger", (builder, a1, a2) -> builder.clause((a3) -> List.of(
				a1.notEquivalent(a2),
				nearView.call(a1, a3),
				nearView.call(a2, a3),
				not(farView.call(a1, a2))
		)));
		var store = ModelStore.builder()
				.symbols(distance)
				.with(QueryInterpreterAdapter.builder()
						.queries(dangerQuery))
				.build();

		var model = store.createEmptyModel();
		var distanceInterpretation = model.getInterpretation(distance);
		distanceInterpretation.put(Tuple.of(0, 1), 1);
		distanceInterpretation.put(Tuple.of(1, 0), 1);
		distanceInterpretation.put(Tuple.of(0, 2), 1);
		distanceInterpretation.put(Tuple.of(2, 0), 1);
		distanceInterpretation.put(Tuple.of(1, 2), 3);
		distanceInterpretation.put(Tuple.of(2, 1), 3);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var dangerResultSet = queryEngine.getResultSet(dangerQuery);
		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1), false,
				Tuple.of(0, 2), false,
				Tuple.of(1, 2), true,
				Tuple.of(2, 1), true
		), dangerResultSet);
	}

	@Test
	void filteredDoubleViewTest() {
		var distance = Symbol.of("distance", 2, Double.class);
		var nearView = new FilteredView<>(distance, value -> value < 2);
		var farView = new FilteredView<>(distance, value -> value >= 5);
		var dangerQuery = Query.of("danger", (builder, a1, a2) -> builder.clause((a3) -> List.of(
				a1.notEquivalent(a2),
				nearView.call(a1, a3),
				nearView.call(a2, a3),
				not(farView.call(a1, a2))
		)));
		var store = ModelStore.builder()
				.symbols(distance)
				.with(QueryInterpreterAdapter.builder()
						.queries(dangerQuery))
				.build();

		var model = store.createEmptyModel();
		var distanceInterpretation = model.getInterpretation(distance);
		distanceInterpretation.put(Tuple.of(0, 1), 1.0);
		distanceInterpretation.put(Tuple.of(1, 0), 1.0);
		distanceInterpretation.put(Tuple.of(0, 2), 1.0);
		distanceInterpretation.put(Tuple.of(2, 0), 1.0);
		distanceInterpretation.put(Tuple.of(1, 2), 3.0);
		distanceInterpretation.put(Tuple.of(2, 1), 3.0);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var dangerResultSet = queryEngine.getResultSet(dangerQuery);
		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0, 1), false,
				Tuple.of(0, 2), false,
				Tuple.of(1, 2), true,
				Tuple.of(2, 1), true
		), dangerResultSet);
	}

	@QueryEngineTest
	void assumeTest(QueryEvaluationHint hint) {
		var age = Symbol.of("age", 1, Integer.class);
		var ageView = new FunctionView<>(age);

		var query = Query.of("Constraint", (builder, p1) -> builder.clause(Integer.class, (x) -> List.of(
				personView.call(p1),
				ageView.call(p1, x),
				check(greaterEq(x, constant(18)))
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

		ageInterpretation.put(Tuple.of(0), 12);
		ageInterpretation.put(Tuple.of(1), 24);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), true,
				Tuple.of(2), false
		), queryResultSet);
	}

	@Test
	void alwaysFalseTest() {
		var predicate = Query.of("AlwaysFalse", builder -> builder.parameter("p1"));

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
		personInterpretation.put(Tuple.of(2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(), predicateResultSet);
	}
}
