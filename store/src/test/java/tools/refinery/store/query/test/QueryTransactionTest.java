package tools.refinery.store.query.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.QueriableModel;
import tools.refinery.store.query.QueriableModelStore;
import tools.refinery.store.query.QueriableModelStoreImpl;
import tools.refinery.store.query.building.DNFAnd;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.RelationAtom;
import tools.refinery.store.query.building.Variable;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.query.view.RelationView;

class QueryTransactionTest {
	@Test
	void flushTest() {
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

		assertEquals(0, model.countResults(predicate));

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(asset, Tuple.of(1), true);
		model.put(asset, Tuple.of(2), true);

		assertEquals(0, model.countResults(predicate));

		model.flushChanges();
		assertEquals(2, model.countResults(predicate));
		
		model.put(person, Tuple.of(4), true);
		assertEquals(2, model.countResults(predicate));
		
		model.flushChanges();
		assertEquals(3, model.countResults(predicate));
	}
}
