package tools.refinery.store.query.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreImpl;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.Tuple.Tuple1;
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
import tools.refinery.store.query.internal.DNF2PQuery;
import tools.refinery.store.query.internal.RawPatternMatcher;
import tools.refinery.store.query.internal.RelationalScope;
import tools.refinery.store.query.view.FilteredRelationView;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.query.view.RelationView;

class QueryTest {
//	@Test
//	void minimalTest() {
//		Relation<Boolean> person = new Relation<>("Person", 1, false);
//
//		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
//		GenericQuerySpecification<GenericPatternMatcher> personQuery = (new PredicateTranslator("PersonQuery"))
//				.addParameter("p", persionView).addConstraint(persionView, "p").build();
//
//		ModelStore store = new ModelStoreImpl(Set.of(person));
//		Model model = store.createModel();
//
//		model.put(person, Tuple.of(0), true);
//		model.put(person, Tuple.of(1), true);
//
//		RelationalScope scope = new RelationalScope(model, Set.of(persionView));
//
//		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
//		GenericPatternMatcher personMatcher = engine.getMatcher(personQuery);
//
//		assertEquals(2, personMatcher.countMatches());
//	}
//
//	void modelBuildingTest() {
//		Relation<Boolean> person = new Relation<>("Person", 1, false);
//		Relation<Integer> age = new Relation<Integer>("age", 1, null);
//		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);
//
//		ModelStore store = new ModelStoreImpl(Set.of(person, age, friend));
//		Model model = store.createModel();
//
//		model.put(person, Tuple.of(0), true);
//		model.put(person, Tuple.of(1), true);
//		model.put(age, Tuple.of(0), 3);
//		model.put(age, Tuple.of(1), 1);
//		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
//		model.put(friend, Tuple.of(1, 0), TruthValue.UNKNOWN);
//
//		// Sanity check
//		assertTrue(model.get(person, Tuple.of(0)));
//		assertTrue(model.get(person, Tuple.of(1)));
//		assertFalse(model.get(person, Tuple.of(2)));
//
//		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
//		RelationView<Integer> ageView = new FunctionalRelationView<>(age);
//		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());
//		RelationView<TruthValue> friendMayView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.may());
//
//		RelationalScope scope = new RelationalScope(model, Set.of(persionView, ageView, friendMustView, friendMayView));
//
//		GenericQuerySpecification<GenericPatternMatcher> personQuery = (new PredicateTranslator("PersonQuery"))
//				.addParameter("p", persionView).addConstraint(persionView, "p").build();
//
//		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
//		GenericPatternMatcher personMatcher = engine.getMatcher(personQuery);
//		Collection<GenericPatternMatch> personMatches = personMatcher.getAllMatches();
//		for (GenericPatternMatch personMatch : personMatches) {
//			System.out.println(personMatch);
//		}
//	}
	
	private void compareMatchSets(Stream<Object[]> matchSet, Set<List<Tuple>> expected) {
		Set<List<Tuple>> translatedMatchSet = new HashSet<>();
		var interator = matchSet.iterator();
		while(interator.hasNext()) {
			var element = interator.next();
			List<Tuple> elementToTranslatedMatchSet = new ArrayList<>();
			for(int i=0; i<element.length; i++) {
				elementToTranslatedMatchSet.add((Tuple) element[i]);
			}
			translatedMatchSet.add(elementToTranslatedMatchSet);
		}
		assertEquals(translatedMatchSet, expected);
	}
	
	@Test
	//@Disabled
	void typeConstraintTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, false);
		Relation<Boolean> asset = new Relation<>("Asset", 1, false);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);

		List<Variable> parameters = Arrays.asList(new Variable("p1"));
		RelationAtom personRelationAtom = new RelationAtom(persionView, parameters);
		DNFAnd clause = new DNFAnd(new HashSet<>(parameters), Arrays.asList(personRelationAtom));
		DNFPredicate predicate = new DNFPredicate("TypeConstraint", parameters, Arrays.asList(clause));
		//GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		//ModelStore store = new ModelStoreImpl(Set.of(person, asset));
		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, asset), Set.of(persionView), Set.of(predicate));
		QueriableModel model = store.createModel();
		
		System.out.println("Res1");
		model.allResults(predicate).forEach(x -> System.out.println(x));
		
		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(asset, Tuple.of(1), true);
		model.put(asset, Tuple.of(2), true);
		
		System.out.println("Res2");
		model.allResults(predicate).forEach(x -> System.out.println(x));
		
		System.out.println("Res3");
		model.flushChanges();
		compareMatchSets(model.allResults(predicate), Set.of(
			List.of(Tuple.of(0)),
			List.of(Tuple.of(1))));
		System.out.println(model.countResults(predicate));
		model.allResults(predicate).forEach(x -> System.out.println(x));
		

		//RelationalScope scope = new RelationalScope(model, Set.of(persionView));

		//ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		//GenericPatternMatcher matcher = engine.getMatcher(query);

		//assertEquals(2, matcher.countMatches());
	}

	@Test
	@Disabled
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
		DNFAnd clause = new DNFAnd(new HashSet<>(parameters),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate predicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		ModelStore store = new ModelStoreImpl(Set.of(person, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView, friendMustView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher matcher = engine.getMatcher(query);

		assertEquals(3, matcher.countMatches());
	}

	@Test
	@Disabled
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
		DNFAnd clause = new DNFAnd(new HashSet<>(parameters),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate friendPredicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		List<Variable> substitution = Arrays.asList(p3, p4);
		RelationAtom personRelationAtom3 = new RelationAtom(persionView, Arrays.asList(p3));
		RelationAtom personRelationAtom4 = new RelationAtom(persionView, Arrays.asList(p4));
		PredicateAtom friendPredicateAtom = new PredicateAtom(true, false, friendPredicate, substitution);
		DNFAnd patternCallClause = new DNFAnd(new HashSet<>(substitution),
				Arrays.asList(personRelationAtom3, personRelationAtom4, friendPredicateAtom));
		DNFPredicate predicate = new DNFPredicate("PatternCall", substitution, Arrays.asList(patternCallClause));

		GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		ModelStore store = new ModelStoreImpl(Set.of(person, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView, friendMustView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher matcher = engine.getMatcher(query);

		assertEquals(3, matcher.countMatches());
	}

	@Test
	@Disabled
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
		DNFAnd clause = new DNFAnd(new HashSet<>(parameters),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate friendPredicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		List<Variable> substitution = Arrays.asList(p3, p4);
		RelationAtom personRelationAtom3 = new RelationAtom(persionView, Arrays.asList(p3));
		RelationAtom personRelationAtom4 = new RelationAtom(persionView, Arrays.asList(p4));
		PredicateAtom friendPredicateAtom = new PredicateAtom(false, false, friendPredicate, substitution);
		DNFAnd negativePatternCallClause = new DNFAnd(new HashSet<>(substitution),
				Arrays.asList(personRelationAtom3, personRelationAtom4, friendPredicateAtom));
		DNFPredicate predicate = new DNFPredicate("NegativePatternCall", substitution,
				Arrays.asList(negativePatternCallClause));

		GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		ModelStore store = new ModelStoreImpl(Set.of(person, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView, friendMustView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher matcher = engine.getMatcher(query);

		assertEquals(6, matcher.countMatches());
	}

	@Test
	@Disabled
	void equalityTest() {
		Relation<Boolean> person = new Relation<Boolean>("Person", 1, false);
		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);

		Variable p1 = new Variable("p1");
		Variable p2 = new Variable("p2");
		List<Variable> parameters = Arrays.asList(p1, p2);

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		EquivalenceAtom equivalenceAtom = new EquivalenceAtom(true, p1, p2);
		DNFAnd clause = new DNFAnd(new HashSet<>(parameters),
				Arrays.asList(personRelationAtom1, personRelationAtom2, equivalenceAtom));
		DNFPredicate predicate = new DNFPredicate("Equality", parameters, Arrays.asList(clause));

		GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		ModelStore store = new ModelStoreImpl(Set.of(person));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher matcher = engine.getMatcher(query);

		assertEquals(3, matcher.countMatches());
	}

	@Test
	@Disabled
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
		DNFAnd clause = new DNFAnd(new HashSet<>(parameters), Arrays.asList(personRelationAtom1, personRelationAtom2,
				friendRelationAtom1, friendRelationAtom2, inequivalenceAtom));
		DNFPredicate predicate = new DNFPredicate("Inequality", parameters, Arrays.asList(clause));

		GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		ModelStore store = new ModelStoreImpl(Set.of(person, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView, friendMustView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher matcher = engine.getMatcher(query);

		assertEquals(2, matcher.countMatches());
	}

	@Test
	@Disabled
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
		DNFAnd clause = new DNFAnd(new HashSet<>(parameters),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom));
		DNFPredicate friendPredicate = new DNFPredicate("RelationConstraint", parameters, Arrays.asList(clause));

		Variable p3 = new Variable("p3");
		Variable p4 = new Variable("p4");
		List<Variable> substitution = Arrays.asList(p3, p4);
		RelationAtom personRelationAtom3 = new RelationAtom(persionView, Arrays.asList(p3));
		RelationAtom personRelationAtom4 = new RelationAtom(persionView, Arrays.asList(p4));
		PredicateAtom friendPredicateAtom = new PredicateAtom(true, true, friendPredicate, substitution);
		DNFAnd patternCallClause = new DNFAnd(new HashSet<>(substitution),
				Arrays.asList(personRelationAtom3, personRelationAtom4, friendPredicateAtom));
		DNFPredicate predicate = new DNFPredicate("TransitivePatternCall", substitution,
				Arrays.asList(patternCallClause));

		GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		ModelStore store = new ModelStoreImpl(Set.of(person, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(person, Tuple.of(2), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 2), TruthValue.TRUE);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView, friendMustView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher matcher = engine.getMatcher(query);

		assertEquals(3, matcher.countMatches());
	}

	@Test
	@Disabled
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

		RelationAtom personRelationAtom1 = new RelationAtom(persionView, Arrays.asList(p1));
		RelationAtom personRelationAtom2 = new RelationAtom(persionView, Arrays.asList(p2));
		RelationAtom friendRelationAtom1 = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause1 = new DNFAnd(new HashSet<>(parameters),
				Arrays.asList(personRelationAtom1, personRelationAtom2, friendRelationAtom1));

		RelationAtom animalRelationAtom1 = new RelationAtom(animalView, Arrays.asList(p1));
		RelationAtom animalRelationAtom2 = new RelationAtom(animalView, Arrays.asList(p2));
		RelationAtom friendRelationAtom2 = new RelationAtom(friendMustView, Arrays.asList(p1, p2));
		DNFAnd clause2 = new DNFAnd(new HashSet<>(parameters),
				Arrays.asList(animalRelationAtom1, animalRelationAtom2, friendRelationAtom2));

		DNFPredicate predicate = new DNFPredicate("Or", parameters, Arrays.asList(clause1, clause2));
		GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(predicate).build();

		ModelStore store = new ModelStoreImpl(Set.of(person, animal, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(animal, Tuple.of(2), true);
		model.put(animal, Tuple.of(3), true);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(0, 2), TruthValue.TRUE);
		model.put(friend, Tuple.of(2, 3), TruthValue.TRUE);
		model.put(friend, Tuple.of(3, 0), TruthValue.TRUE);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView, animalView, friendMustView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher matcher = engine.getMatcher(query);

		assertEquals(2, matcher.countMatches());
	}
}