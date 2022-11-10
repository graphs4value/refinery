package tools.refinery.store.query.viatra.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.*;
import tools.refinery.store.query.atom.*;
import tools.refinery.store.query.viatra.ViatraQueryableModelStore;
import tools.refinery.store.query.view.FilteredRelationView;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.query.view.RelationView;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTest {
	@Test
	void typeConstraintTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<Boolean> asset = new Relation<>("Asset", 1, Boolean.class, false);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);

		var p1 = new Variable("p1");
		DNF predicate = DNF.builder("TypeConstraint")
				.parameters(p1)
				.clause(new RelationViewAtom(personView, p1))
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, asset), Set.of(personView),
				Set.of(predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(asset, Tuple.of(1), true);
		model.put(asset, Tuple.of(2), true);

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0), Tuple.of(1)));
	}

	@Test
	void relationConstraintTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF predicate = DNF.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(predicate));
		QueryableModel model = store.createModel();

		assertEquals(0, model.countResults(predicate));

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		assertEquals(0, model.countResults(predicate));

		model.flushChanges();
		assertEquals(3, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0, 1), Tuple.of(1, 0), Tuple.of(1, 2)));
	}

	@Test
	void andTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF predicate = DNF.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2),
						new RelationViewAtom(friendMustView, p2, p1)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(predicate));
		QueryableModel model = store.createModel();

		assertEquals(0, model.countResults(predicate));

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);

		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(0, model.countResults(predicate));

		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0, 1), Tuple.of(1, 0)));

		model.put(friend, Tuple.of(2, 0), TruthValue.TRUE);
		model.flushChanges();
		assertEquals(4, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0, 1), Tuple.of(1, 0), Tuple.of(0, 2),
				Tuple.of(2, 0)));
	}

	@Test
	void existTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF predicate = DNF.builder("RelationConstraint")
				.parameters(p1)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(predicate));
		QueryableModel model = store.createModel();

		assertEquals(0, model.countResults(predicate));

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		assertEquals(0, model.countResults(predicate));

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0), Tuple.of(1)));
	}

	@Test
	void orTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<Boolean> animal = new Relation<>("Animal", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<Boolean> animalView = new KeyOnlyRelationView(animal);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF predicate = DNF.builder("Or")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.clause(
						new RelationViewAtom(animalView, p1),
						new RelationViewAtom(animalView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, animal, friend),
				Set.of(personView, animalView, friendMustView), Set.of(predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(animal, Tuple.of(2), true);
		model.put(animal, Tuple.of(3), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);
		model.put(friend, Tuple.of(2, 3), TruthValue.TRUE);
		model.put(friend, Tuple.of(3, 0), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0, 1), Tuple.of(2, 3)));
	}

	@Test
	void equalityTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF predicate = DNF.builder("Equality")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new EquivalenceAtom(p1, p2)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person), Set.of(personView), Set.of(predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);

		model.flushChanges();
		assertEquals(3, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0, 0), Tuple.of(1, 1), Tuple.of(2, 2)));
	}

	@Test
	void inequalityTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		Variable p3 = new Variable("p3");
		DNF predicate = DNF.builder("Inequality")
				.parameters(p1, p2, p3)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p3),
						new RelationViewAtom(friendMustView, p2, p3),
						new EquivalenceAtom(false, p1, p2)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(Tuple.of(0, 1, 2), Tuple.of(1, 0, 2)));
	}

	@Test
	void patternCallTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF friendPredicate = DNF.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		DNF predicate = DNF.builder("PositivePatternCall")
				.parameters(p3, p4)
				.clause(
						new RelationViewAtom(personView, p3),
						new RelationViewAtom(personView, p4),
						new CallAtom<>(friendPredicate, p3, p4)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(friendPredicate, predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		model.flushChanges();

		assertEquals(3, model.countResults(friendPredicate));
	}

	@Test
	void negativePatternCallTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF friendPredicate = DNF.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		DNF predicate = DNF.builder("NegativePatternCall")
				.parameters(p3, p4)
				.clause(
						new RelationViewAtom(personView, p3),
						new RelationViewAtom(personView, p4),
						new CallAtom<>(false, friendPredicate, p3, p4)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(friendPredicate, predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(6, model.countResults(predicate));
	}

	@Test
	void negativeWithQuantificationTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");

		DNF called = DNF.builder("Called")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		DNF predicate = DNF.builder("Count")
				.parameters(p1)
				.clause(
						new RelationViewAtom(personView, p1),
						new CallAtom<>(false, called, p1, p2)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(called, predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
	}

	@Test
	void transitivePatternCallTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		DNF friendPredicate = DNF.builder("RelationConstraint")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		DNF predicate = DNF.builder("TransitivePatternCall")
				.parameters(p3, p4)
				.clause(
						new RelationViewAtom(personView, p3),
						new RelationViewAtom(personView, p4),
						new CallAtom<>(SimplePolarity.TRANSITIVE, friendPredicate, p3, p4)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(friendPredicate, predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(3, model.countResults(predicate));
	}

	@Test
	void countMatchTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, Boolean.class, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.class, TruthValue.FALSE);
		RelationView<Boolean> personView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<>(friend, "must",
				TruthValue::must);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");

		DNF called = DNF.builder("Called")
				.parameters(p1, p2)
				.clause(
						new RelationViewAtom(personView, p1),
						new RelationViewAtom(personView, p2),
						new RelationViewAtom(friendMustView, p1, p2)
				)
				.build();

		DNF predicate = DNF.builder("Count")
				.parameters(p1)
				.clause(
						new RelationViewAtom(personView, p1),
						new CallAtom<>(new CountingPolarity(ComparisonOperator.EQUALS, 2), called, p1, p2)
				)
				.build();

		QueryableModelStore store = new ViatraQueryableModelStore(Set.of(person, friend),
				Set.of(personView, friendMustView), Set.of(called, predicate));
		QueryableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(1, model.countResults(predicate));
	}

	static void compareMatchSets(Stream<TupleLike> matchSet, Set<Tuple> expected) {
		Set<Tuple> translatedMatchSet = new HashSet<>();
		var iterator = matchSet.iterator();
		while (iterator.hasNext()) {
			var element = iterator.next();
			translatedMatchSet.add(element.toTuple());
		}
		assertEquals(expected, translatedMatchSet);
	}
}
