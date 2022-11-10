package tools.refinery.store.query.viatra.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.DNF;
import tools.refinery.store.query.QueryableModel;
import tools.refinery.store.query.QueryableModelStore;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.atom.RelationViewAtom;
import tools.refinery.store.query.viatra.ViatraQueryableModelStore;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.query.view.RelationView;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTransactionTest {
	@Test
	void flushTest() {
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
