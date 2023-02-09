package tools.refinery.store.query.viatra;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.ModelQuery;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.*;

class QueryTransactionTest {
	@Test
	void flushTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var asset = new Symbol<>("Asset", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);

		var p1 = new Variable("p1");
		var predicate = Dnf.builder("TypeConstraint")
				.parameters(p1)
				.clause(personView.call(p1))
				.build();

		var store = ModelStore.builder()
				.symbols(person, asset)
				.with(ViatraModelQuery.ADAPTER)
				.queries(predicate)
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var assetInterpretation = model.getInterpretation(asset);
		var queryEngine = model.getAdapter(ModelQuery.ADAPTER);
		var predicateResultSet = queryEngine.getResultSet(predicate);

		assertEquals(0, predicateResultSet.countResults());
		assertFalse(queryEngine.hasPendingChanges());

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		assetInterpretation.put(Tuple.of(1), true);
		assetInterpretation.put(Tuple.of(2), true);

		assertEquals(0, predicateResultSet.countResults());
		assertTrue(queryEngine.hasPendingChanges());

		queryEngine.flushChanges();
		assertEquals(2, predicateResultSet.countResults());
		assertFalse(queryEngine.hasPendingChanges());

		personInterpretation.put(Tuple.of(4), true);
		assertEquals(2, predicateResultSet.countResults());
		assertTrue(queryEngine.hasPendingChanges());

		queryEngine.flushChanges();
		assertEquals(3, predicateResultSet.countResults());
		assertFalse(queryEngine.hasPendingChanges());
	}
}
