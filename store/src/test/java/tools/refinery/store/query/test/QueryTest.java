package tools.refinery.store.query.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.QueriableModel;
import tools.refinery.store.query.QueriableModelStore;
import tools.refinery.store.query.QueriableModelStoreImpl;
import tools.refinery.store.query.building.DNFAnd;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.EquivalenceAtom;
import tools.refinery.store.query.building.PredicateAtom;
import tools.refinery.store.query.building.RelationAtom;
import tools.refinery.store.query.building.Variable;
import tools.refinery.store.query.view.FilteredRelationView;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.query.view.RelationView;

class QueryTest {

	static void compareMatchSets(Stream<Object[]> matchSet, Set<List<Tuple>> expected) {
		Set<List<Tuple>> translatedMatchSet = new HashSet<>();
		var interator = matchSet.iterator();
		while (interator.hasNext()) {
			var element = interator.next();
			List<Tuple> elementToTranslatedMatchSet = new ArrayList<>();
			for (int i = 0; i < element.length; i++) {
				elementToTranslatedMatchSet.add((Tuple) element[i]);
			}
			translatedMatchSet.add(elementToTranslatedMatchSet);
		}

		assertEquals(expected, translatedMatchSet);
	}

	@Test
	void typeConstraintTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, false);
		Relation<Boolean> asset = new Relation<>("Asset", 1, false);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);

		List<Variable> parameters = Arrays.asList(new Variable("p1"));
		RelationAtom personRelationAtom = new RelationAtom(persionView, parameters);
		DNFAnd clause = new DNFAnd(Collections.emptySet(), Arrays.asList(personRelationAtom));
		DNFPredicate predicate = new DNFPredicate("TypeConstraint", parameters, Arrays.asList(clause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, asset), Set.of(persionView),
				Set.of(predicate));
		QueriableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(asset, Tuple.of(1), true);
		model.put(asset, Tuple.of(2), true);

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(List.of(Tuple.of(0)), List.of(Tuple.of(1))));
	}

	@Test
	void relationConstraintTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate predicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(predicate));
		QueriableModel model = store.createModel();

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
		compareMatchSets(model.allResults(predicate), Set.of(List.of(Tuple.of(0), Tuple.of(1)),
				List.of(Tuple.of(1), Tuple.of(0)), List.of(Tuple.of(1), Tuple.of(2))));
	}

	@Test
	void andTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom1 = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		RelationAtom friendRelationAtom2 = new RelationAtom(friendMustView, Arrays.asList(p2, p1));
		DNFAnd clause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom1, friendRelationAtom2));
		DNFPredicate predicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(predicate));
		QueriableModel model = store.createModel();

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
		compareMatchSets(model.allResults(predicate),
				Set.of(List.of(Tuple.of(0), Tuple.of(1)), List.of(Tuple.of(1), Tuple.of(0))));

		model.put(friend, Tuple.of(2, 0), TruthValue.TRUE);
		model.flushChanges();
		assertEquals(4, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate),
				Set.of(List.of(Tuple.of(0), Tuple.of(1)), List.of(Tuple.of(1), Tuple.of(0)),
						List.of(Tuple.of(0), Tuple.of(2)), List.of(Tuple.of(2), Tuple.of(0))));
	}

	@Test
	void existTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause = new DNFAnd(Set.of(p2),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate predicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(predicate));
		QueriableModel model = store.createModel();

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
		compareMatchSets(model.allResults(predicate), Set.of(List.of(Tuple.of(0)), List.of(Tuple.of(1))));
	}

	@Test
	void orTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, false);
		Relation<Boolean> animal = new Relation<>("Animal", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<Boolean> animalView = new KeyOnlyRelationView(animal);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		// Person-Person friendship
		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom1 = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause1 = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom1));

		// Animal-Animal friendship
		RelationAtom animalRelationAtom1 = new RelationAtom(animalView, Arrays.asList(p1));
		RelationAtom animalRelationAtom2 = new RelationAtom(animalView, Arrays.asList(p2));
		RelationAtom friendRelationAtom2 = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause2 = new DNFAnd(Collections.emptySet(),
				Arrays.asList(animalRelationAtom1, animalRelationAtom2, friendRelationAtom2));

		// No inter-species friendship

		DNFPredicate predicate = new DNFPredicate("Or", parameters, Arrays.asList(clause1, clause2));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, animal, friend),
				Set.of(persionView, animalView, friendMustView), Set.of(predicate));
		QueriableModel model = store.createModel();

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
		compareMatchSets(model.allResults(predicate),
				Set.of(List.of(Tuple.of(0), Tuple.of(1)), List.of(Tuple.of(2), Tuple.of(3))));
	}

	@Test
	void equalityTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		EquivalenceAtom equivalenceAtom = new EquivalenceAtom(true, p1, p2);
		DNFAnd clause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom1, personRelationAtom2, equivalenceAtom));
		DNFPredicate predicate = new DNFPredicate("Equality", parameters, Arrays.asList(clause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person), Set.of(persionView), Set.of(predicate));
		QueriableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);

		model.flushChanges();
		assertEquals(3, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate), Set.of(List.of(Tuple.of(0), Tuple.of(0)),
				List.of(Tuple.of(1), Tuple.of(1)), List.of(Tuple.of(2), Tuple.of(2))));
	}

	@Test
	void inequalityTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		Variable p3 = new Variable("p3");
		List<Variable> parameters = Arrays.asList(p1, p2, p3);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom1 = new RelationAtom(friendMustView, Arrays.asList(p1, p3));
		RelationAtom friendRelationAtom2 = new RelationAtom(friendMustView, Arrays.asList(p2, p3));
		EquivalenceAtom inequivalenceAtom = new EquivalenceAtom(false, p1, p2);
		DNFAnd clause = new DNFAnd(Collections.emptySet(), Arrays.asList(personRelationAtom1, personRelationAtom2,
				friendRelationAtom1, friendRelationAtom2, inequivalenceAtom));
		DNFPredicate predicate = new DNFPredicate("Inequality", parameters, Arrays.asList(clause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(predicate));
		QueriableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		compareMatchSets(model.allResults(predicate),
				Set.of(List.of(Tuple.of(0), Tuple.of(1), Tuple.of(2)), List.of(Tuple.of(1), Tuple.of(0), Tuple.of(2))));
	}

	@Test
	void patternCallTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate friendPredicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		List<Variable> substitution = Arrays.asList(p3, p4);
		RelationAtom personRelationAtom3 = new RelationAtom(persionView, Arrays.asList(p3));
		RelationAtom personRelationAtom4 = new RelationAtom(persionView, Arrays.asList(p4));
		PredicateAtom friendPredicateAtom = new PredicateAtom(true, false, friendPredicate, substitution);
		DNFAnd patternCallClause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom3, personRelationAtom4, friendPredicateAtom));
		DNFPredicate predicate = new DNFPredicate("PatternCall", substitution, Arrays.asList(patternCallClause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(friendPredicate, predicate));
		QueriableModel model = store.createModel();

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
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate friendPredicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		List<Variable> substitution = Arrays.asList(p3, p4);
		RelationAtom personRelationAtom3 = new RelationAtom(persionView, Arrays.asList(p3));
		RelationAtom personRelationAtom4 = new RelationAtom(persionView, Arrays.asList(p4));
		PredicateAtom friendPredicateAtom = new PredicateAtom(false, false, friendPredicate, substitution);
		DNFAnd negativePatternCallClause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom3, personRelationAtom4, friendPredicateAtom));
		DNFPredicate predicate = new DNFPredicate("NegativePatternCall", substitution,
				Arrays.asList(negativePatternCallClause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(friendPredicate, predicate));
		QueriableModel model = store.createModel();

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
	void transitivePatternCallTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate friendPredicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		List<Variable> substitution = Arrays.asList(p3, p4);
		RelationAtom personRelationAtom3 = new RelationAtom(persionView, Arrays.asList(p3));
		RelationAtom personRelationAtom4 = new RelationAtom(persionView, Arrays.asList(p4));
		PredicateAtom friendPredicateAtom = new PredicateAtom(true, true, friendPredicate, substitution);
		DNFAnd patternCallClause = new DNFAnd(Collections.emptySet(),
				Arrays.asList(personRelationAtom3, personRelationAtom4, friendPredicateAtom));
		DNFPredicate predicate = new DNFPredicate("TransitivePatternCall", substitution,
				Arrays.asList(patternCallClause));

		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(friendPredicate, predicate));
		QueriableModel model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);
		
		model.flushChanges();
		assertEquals(3, model.countResults(predicate));
	}
}