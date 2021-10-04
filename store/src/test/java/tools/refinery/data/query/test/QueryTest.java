package tools.refinery.data.query.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.GenericPatternMatch;
import org.eclipse.viatra.query.runtime.api.GenericPatternMatcher;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.junit.jupiter.api.Test;

import tools.refinery.data.model.Model;
import tools.refinery.data.model.ModelStore;
import tools.refinery.data.model.ModelStoreImpl;
import tools.refinery.data.model.Tuple;
import tools.refinery.data.model.representation.Relation;
import tools.refinery.data.model.representation.TruthValue;
import tools.refinery.data.query.RelationalScope;
import tools.refinery.data.query.internal.PredicateTranslator;
import tools.refinery.data.query.view.FilteredRelationView;
import tools.refinery.data.query.view.FunctionalRelationView;
import tools.refinery.data.query.view.KeyOnlyRelationView;
import tools.refinery.data.query.view.RelationView;

class QueryTest {
	@Test
	void minimalTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, false);

		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		GenericQuerySpecification<GenericPatternMatcher> personQuery = (new PredicateTranslator("PersonQuery"))
				.addParameter("p", persionView).addConstraint(persionView, "p").build();

		ModelStore store = new ModelStoreImpl(Set.of(person));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);

		RelationalScope scope = new RelationalScope(model, Set.of(persionView));

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher personMatcher = engine.getMatcher(personQuery);

		assertEquals(2, personMatcher.countMatches());
	}

	void modelBuildingTest() {
		Relation<Boolean> person = new Relation<>("Person", 1, false);
		Relation<Integer> age = new Relation<Integer>("age", 1, null);
		Relation<TruthValue> friend = new Relation<>("friend", 2, TruthValue.FALSE);

		ModelStore store = new ModelStoreImpl(Set.of(person, age, friend));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(age, Tuple.of(0), 3);
		model.put(age, Tuple.of(1), 1);
		model.put(friend, Tuple.of(0, 1), TruthValue.TRUE);
		model.put(friend, Tuple.of(1, 0), TruthValue.UNKNOWN);

		// Sanity check
		assertTrue(model.get(person, Tuple.of(0)));
		assertTrue(model.get(person, Tuple.of(1)));
		assertFalse(model.get(person, Tuple.of(2)));

		RelationView<Boolean> persionView = new KeyOnlyRelationView(person);
		RelationView<Integer> ageView = new FunctionalRelationView<>(age);
		RelationView<TruthValue> friendMustView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.must());
		RelationView<TruthValue> friendMayView = new FilteredRelationView<TruthValue>(friend, (k, v) -> v.may());

		RelationalScope scope = new RelationalScope(model, Set.of(persionView, ageView, friendMustView, friendMayView));

		GenericQuerySpecification<GenericPatternMatcher> personQuery = (new PredicateTranslator("PersonQuery"))
				.addParameter("p", persionView).addConstraint(persionView, "p").build();

		ViatraQueryEngine engine = AdvancedViatraQueryEngine.on(scope);
		GenericPatternMatcher personMatcher = engine.getMatcher(personQuery);
		Collection<GenericPatternMatch> personMatches = personMatcher.getAllMatches();
		for (GenericPatternMatch personMatch : personMatches) {
			System.out.println(personMatch);
		}
	}
}