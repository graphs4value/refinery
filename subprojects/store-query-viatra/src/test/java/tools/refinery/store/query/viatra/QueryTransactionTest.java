package tools.refinery.store.query.viatra;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.DNF;
import tools.refinery.store.query.ModelQuery;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.atom.RelationViewAtom;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTransactionTest {
	@Test
	void flushTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var asset = new Symbol<>("Asset", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);

		var p1 = new Variable("p1");
		var predicate = DNF.builder("TypeConstraint")
				.parameters(p1)
				.clause(new RelationViewAtom(personView, p1))
				.build();

		var store = ModelStore.builder()
				.symbols(person, asset)
				.with(ViatraModelQuery.ADAPTER)
				.queries(predicate)
				.build();

		var model = store.createModel();
		var personInterpretation = model.getInterpretation(person);
		var assetInterpretation = model.getInterpretation(asset);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		assertEquals(0, predicateResultSet.countResults());

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		assetInterpretation.put(Tuple.of(1), true);
		assetInterpretation.put(Tuple.of(2), true);

		assertEquals(0, predicateResultSet.countResults());

		queryEngine.flushChanges();
		assertEquals(2, predicateResultSet.countResults());

		personInterpretation.put(Tuple.of(4), true);
		assertEquals(2, predicateResultSet.countResults());

		queryEngine.flushChanges();
		assertEquals(3, predicateResultSet.countResults());
	}
}
