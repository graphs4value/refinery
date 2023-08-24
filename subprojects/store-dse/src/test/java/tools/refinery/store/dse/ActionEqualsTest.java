package tools.refinery.store.dse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.internal.action.*;
import tools.refinery.store.dse.strategy.DepthFirstStrategy;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ActionEqualsTest {

	private static Model model;
	private static DesignSpaceExplorationAdapter dseAdapter;
	private static Symbol<Boolean> type1;
	private static Symbol<Boolean> type2;
	private static Symbol<TruthValue> type3;
	private static Symbol<Boolean> relation1;
	private static Symbol<Boolean> relation2;
	private static RelationalQuery precondition1;
	private static RelationalQuery precondition2;

	@BeforeAll
	public static void init() {
		type1 = Symbol.of("type1", 1);
		type2 = Symbol.of("type2", 1);
		type3 = Symbol.of("type3", 1, TruthValue.class);
		relation1 = Symbol.of("relation1", 2);
		relation2 = Symbol.of("relation2", 2);
		AnySymbolView type1View = new KeyOnlyView<>(type1);
		precondition1 = Query.of("CreateClassPrecondition",
				(builder, model) -> builder.clause(
						type1View.call(model)
				));

		precondition2 = Query.of("CreateFeaturePrecondition",
				(builder, model) -> builder.clause(
						type1View.call(model)
				));
		var store = ModelStore.builder()
				.symbols(type1, type2, type3, relation2, relation1)
				.with(ViatraModelQueryAdapter.builder()
						.queries(precondition1, precondition2))
				.with(DesignSpaceExplorationAdapter.builder()
						.strategy(new DepthFirstStrategy()))
				.build();



		model = store.createEmptyModel();
		dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
	}

	@Test
	void emptyActionEqualsTest() {
		var action1 = new TransformationAction();
		var action2 = new TransformationAction();
		assertEquals(action1, action1);
		assertEquals(action2, action2);
		assertEquals(action1, action2);
	}

	@Test
	void actionTrivialTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol);
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol);
		action1.add(insertAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol1);
		action2.add(activationSymbol);
		action2.add(insertAction2);
		action2.prepare(model);

		assertEquals(action1, action2);
	}

	@Test
	void actionIdenticalTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1, activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);


		var newItemSymbol2 = new NewItemSymbol();
		var activationSymbol2 = new ActivationSymbol();
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol2,
				activationSymbol2);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol2);
		action2.add(activationSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertEquals(action1, action2);
	}

	@Test
	void actionSymbolGlobalOrderTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1, activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);


		var newItemSymbol2 = new NewItemSymbol();
		var activationSymbol2 = new ActivationSymbol();
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol2,
				activationSymbol2);

		var action2 = new TransformationAction();
		action2.add(activationSymbol2);
		action2.add(newItemSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertNotEquals(action1, action2);
	}

	@Test
	void actionSymbolInInsertActionOrderTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1, activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);


		var newItemSymbol2 = new NewItemSymbol();
		var activationSymbol2 = new ActivationSymbol();
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, activationSymbol2,
				newItemSymbol2);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol2);
		action2.add(activationSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertNotEquals(action1, action2);
	}

	@Test
	void identicalInsertActionInDifferentOrderTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.add(insertAction2);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol1);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.add(insertAction1);
		action2.prepare(model);

		assertEquals(action1, action2);
	}

	@Test
	void identicalActionAndSymbolDifferentOrderTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);

		var newItemSymbol2 = new NewItemSymbol();
		var activationSymbol2 = new ActivationSymbol();
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol2,
				activationSymbol2);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(newItemSymbol2);
		action1.add(activationSymbol1);
		action1.add(activationSymbol2);
		action1.add(insertAction1);
		action1.add(insertAction2);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol2);
		action2.add(newItemSymbol1);
		action2.add(activationSymbol2);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.add(insertAction1);
		action2.prepare(model);

		assertEquals(action1, action2);
	}

	@Test
	void identicalActionAndSymbolMixedOrderTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);

		var newItemSymbol2 = new NewItemSymbol();
		var activationSymbol2 = new ActivationSymbol();
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol2,
				activationSymbol2);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(newItemSymbol2);
		action1.add(activationSymbol1);
		action1.add(activationSymbol2);
		action1.add(insertAction1);
		action1.add(insertAction2);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(insertAction1);
		action2.add(newItemSymbol1);
		action2.add(newItemSymbol2);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.add(activationSymbol2);
		action2.prepare(model);

		assertEquals(action1, action2);
	}

	@Test
	void insertActionInterpretationTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(type2), true, newItemSymbol1,
				activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol1);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.prepare(model);

		assertNotEquals(action1, action2);
	}

	@Test
	void insertActionValueTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), false, newItemSymbol1,
				activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol1);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.prepare(model);

		assertNotEquals(action1, action2);
	}

	@Test
	void newItemSymbolDuplicateTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var newItemSymbol2 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol2,
				activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol2);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.prepare(model);

		assertEquals(action1, action2);
	}

	@Test
	void activationSymbolDuplicateTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol();
		var activationSymbol2 = new ActivationSymbol();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol2);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol1);
		action2.add(activationSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertEquals(action1, action2);
	}

	@Test
	void activationSymbolIndexTest() {
		var newItemSymbol1 = new NewItemSymbol();
		var activationSymbol1 = new ActivationSymbol(0);
		var activationSymbol2 = new ActivationSymbol(1);
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1,
				activationSymbol2);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol1);
		action2.add(activationSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertNotEquals(action1, action2);
	}
}
