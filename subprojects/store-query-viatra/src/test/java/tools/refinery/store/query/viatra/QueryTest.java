/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.literal.Literals.assume;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.greaterEq;
import static tools.refinery.store.query.viatra.tests.QueryAssertions.assertResults;

class QueryTest {
	@QueryEngineTest
	void typeConstraintTest(QueryEvaluationHint hint) {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var asset = new Symbol<>("Asset", 1, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);

		var p1 = Variable.of("p1");
		var predicate = Query.builder("TypeConstraint")
				.parameters(p1)
				.clause(personView.call(p1))
				.build();

		var store = ModelStore.builder()
				.symbols(person, asset)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.query(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var assetInterpretation = model.getInterpretation(asset);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var predicate = Query.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
	void existTest(QueryEvaluationHint hint) {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var predicate = Query.builder("RelationConstraint")
				.parameters(p1)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var animal = new Symbol<>("Animal", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var animalView = new KeyOnlyView<>(animal);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var predicate = Query.builder("Or")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.clause(
						animalView.call(p1),
						animalView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, animal, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var animalInterpretation = model.getInterpretation(animal);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var predicate = Query.builder("Equality")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						p1.isEquivalent(p2)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var p3 = Variable.of("p3");
		var predicate = Query.builder("Inequality")
				.parameters(p1, p2, p3)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p3),
						friendMustView.call(p2, p3),
						p1.notEquivalent(p2)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var friendPredicate = Dnf.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();

		var p3 = Variable.of("p3");
		var p4 = Variable.of("p4");
		var predicate = Query.builder("PositivePatternCall")
				.parameters(p3, p4)
				.clause(
						personView.call(p3),
						personView.call(p4),
						friendPredicate.call(p3, p4)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var predicate = Query.builder("NegativePatternCall")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						not(friendMustView.call(p1, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var friendPredicate = Dnf.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();

		var p3 = Variable.of("p3");
		var p4 = Variable.of("p4");
		var predicate = Query.builder("NegativePatternCall")
				.parameters(p3, p4)
				.clause(
						personView.call(p3),
						personView.call(p4),
						not(friendPredicate.call(p3, p4))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");

		var predicate = Query.builder("Count")
				.parameters(p1)
				.clause(
						personView.call(p1),
						not(friendMustView.call(p1, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");

		var called = Dnf.builder("Called")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();

		var predicate = Query.builder("Count")
				.parameters(p1)
				.clause(
						personView.call(p1),
						not(called.call(p1, p2))
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var predicate = Query.builder("TransitivePatternCall")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.callTransitive(p1, p2)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		var personView = new KeyOnlyView<>(person);
		var friendMustView = new FilteredView<>(friend, "must", TruthValue::must);

		var p1 = Variable.of("p1");
		var p2 = Variable.of("p2");
		var friendPredicate = Dnf.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						personView.call(p1),
						personView.call(p2),
						friendMustView.call(p1, p2)
				)
				.build();

		var p3 = Variable.of("p3");
		var p4 = Variable.of("p4");
		var predicate = Query.builder("TransitivePatternCall")
				.parameters(p3, p4)
				.clause(
						personView.call(p3),
						personView.call(p4),
						friendPredicate.callTransitive(p3, p4)
				)
				.build();

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQuery.ADAPTER)
				.defaultHint(hint)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
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
	void assumeTest(QueryEvaluationHint hint) {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var personView = new KeyOnlyView<>(person);
		var ageView = new FunctionView<>(age);

		var p1 = Variable.of("p1");
		var x = Variable.of("x", Integer.class);
		var query = Query.builder("Constraint")
				.parameter(p1)
				.clause(
						personView.call(p1),
						ageView.call(p1, x),
						assume(greaterEq(x, constant(18)))
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
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), true,
				Tuple.of(2), false
		), queryResultSet);
	}

	@Test
	void alwaysFalseTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);

		var p1 = Variable.of("p1");
		var predicate = Query.builder("AlwaysFalse").parameters(p1).build();

		var store = ModelStore.builder()
				.symbols(person)
				.with(ViatraModelQuery.ADAPTER)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		queryEngine.flushChanges();
		assertResults(Map.of(), predicateResultSet);
	}

	@Test
	void alwaysTrueTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);

		var p1 = Variable.of("p1");
		var predicate = Query.builder("AlwaysTrue").parameters(p1).clause().build();

		var storeBuilder = ModelStore.builder().symbols(person);
		var queryBuilder = storeBuilder.with(ViatraModelQuery.ADAPTER);

		assertThrows(IllegalArgumentException.class, () -> queryBuilder.queries(predicate));
	}
}
