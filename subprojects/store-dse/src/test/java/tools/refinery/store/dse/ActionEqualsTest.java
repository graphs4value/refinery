/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.internal.action.*;
import tools.refinery.store.dse.strategy.DepthFirstStrategy;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionEqualsTest {

	private static Model model;
	private static Symbol<Boolean> type1;
	private static Symbol<Boolean> relation1;
	private static Symbol<Boolean> relation2;

	@BeforeAll
	public static void init() {
		type1 = Symbol.of("type1", 1);
		relation1 = Symbol.of("relation1", 2);
		relation2 = Symbol.of("relation2", 2);
		var type1View = new KeyOnlyView<>(type1);
		var precondition1 = Query.of("CreateClassPrecondition",
				(builder, model) -> builder.clause(
						type1View.call(model)
				));

		var precondition2 = Query.of("CreateFeaturePrecondition",
				(builder, model) -> builder.clause(
						type1View.call(model)
				));
		var store = ModelStore.builder()
				.symbols(type1, relation2, relation1)
				.with(ViatraModelQueryAdapter.builder()
						.queries(precondition1, precondition2))
				.with(DesignSpaceExplorationAdapter.builder()
						.strategy(new DepthFirstStrategy()))
				.build();


		model = store.createEmptyModel();
	}

	@Test
	void emptyActionEqualsTest() {
		var action1 = new TransformationAction();
		var action2 = new TransformationAction();

		assertTrue(action1.equalsWithSubstitution(action1));
		assertTrue(action2.equalsWithSubstitution(action2));
		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void actionTrivialTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol);
		var insertAction3 = new InsertAction<>(model.getInterpretation(type1), true, newItemSymbol1);
		var insertAction4 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol);
		action1.add(insertAction1);
		action1.add(insertAction2);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol1);
		action2.add(activationSymbol);
		action2.add(insertAction3);
		action2.add(insertAction4);
		action2.prepare(model);

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void actionIdenticalTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol2 = new ActivationVariable();
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
				activationSymbol2);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol2);
		action2.add(activationSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void actionSymbolGlobalOrderTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(activationSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol2 = new ActivationVariable();
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
				activationSymbol2);

		var action2 = new TransformationAction();
		action2.add(activationSymbol2);
		action2.add(newItemSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertFalse(action1.equalsWithSubstitution(action2));
	}

	@Test
	void actionSymbolRepeatedInInsertActionTest() {
		var newItemSymbol1 = new NewItemVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				newItemSymbol1);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(insertAction1);
		action1.prepare(model);

		var newItemSymbol2 = new NewItemVariable();
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
				newItemSymbol2);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol2);
		action2.add(insertAction2);
		action2.prepare(model);

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void identicalInsertActionInDifferentOrderTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
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

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void identicalActionAndSymbolDifferentOrderTest() {
		var newItemSymbol1 = new NewItemVariable();
		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var activationSymbol2 = new ActivationVariable();

		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);

		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
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

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void identicalActionAndSymbolMixedOrderTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);

		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol2 = new ActivationVariable();
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
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

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void insertActionInterpretationTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation2), true, newItemSymbol1,
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

		assertFalse(action1.equalsWithSubstitution(action2));
	}

	@Test
	void insertActionValueTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), false, newItemSymbol1,
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

		assertFalse(action1.equalsWithSubstitution(action2));
	}

	@Test
	void newItemSymbolDuplicateTest() {
		var newItemSymbol1 = new NewItemVariable();
		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
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

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void activationSymbolDuplicateTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var activationSymbol2 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
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

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void activationSymbolIndexTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable(0);
		var activationSymbol2 = new ActivationVariable(1);
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
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

		assertFalse(action1.equalsWithSubstitution(action2));
	}

	@Test
	void deleteActionTest() {
		var newItemSymbol = new NewItemVariable();
		var activationSymbol = new ActivationVariable(0);
		var insertAction = new InsertAction<>(model.getInterpretation(relation1), false, newItemSymbol,
				activationSymbol);
		var deleteAction = new DeleteAction(activationSymbol);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol);
		action1.add(activationSymbol);
		action1.add(insertAction);
		action1.add(deleteAction);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol);
		action2.add(activationSymbol);
		action2.add(insertAction);
		action2.add(deleteAction);
		action2.prepare(model);

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void deleteActionMissingTest() {
		var newItemSymbol = new NewItemVariable();
		var activationSymbol = new ActivationVariable(0);
		var insertAction = new InsertAction<>(model.getInterpretation(relation1), false, newItemSymbol,
				activationSymbol);
		var deleteAction = new DeleteAction(activationSymbol);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol);
		action1.add(activationSymbol);
		action1.add(insertAction);
		action1.add(deleteAction);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol);
		action2.add(activationSymbol);
		action2.add(insertAction);
		action2.prepare(model);

		assertFalse(action1.equalsWithSubstitution(action2));
	}

	@Test
	void deleteActionIdenticalTest() {
		var newItemSymbol = new NewItemVariable();
		var activationSymbol = new ActivationVariable(0);
		var insertAction = new InsertAction<>(model.getInterpretation(relation1), false, newItemSymbol,
				activationSymbol);
		var deleteAction1 = new DeleteAction(activationSymbol);
		var deleteAction2 = new DeleteAction(activationSymbol);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol);
		action1.add(activationSymbol);
		action1.add(insertAction);
		action1.add(deleteAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol);
		action2.add(activationSymbol);
		action2.add(insertAction);
		action2.add(deleteAction2);
		action2.prepare(model);

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void deleteActionSymbolTypeTest() {
		var newItemSymbol = new NewItemVariable();
		var activationSymbol = new ActivationVariable(0);
		var insertAction = new InsertAction<>(model.getInterpretation(relation1), false, newItemSymbol,
				activationSymbol);
		var deleteAction1 = new DeleteAction(activationSymbol);
		var deleteAction2 = new DeleteAction(newItemSymbol);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol);
		action1.add(activationSymbol);
		action1.add(insertAction);
		action1.add(deleteAction1);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol);
		action2.add(activationSymbol);
		action2.add(insertAction);
		action2.add(deleteAction2);
		action2.prepare(model);

		assertFalse(action1.equalsWithSubstitution(action2));
	}

	@Test
	void deleteActionOrderTest() {
		var newItemSymbol = new NewItemVariable();
		var activationSymbol = new ActivationVariable(0);
		var insertAction = new InsertAction<>(model.getInterpretation(relation1), false, newItemSymbol,
				activationSymbol);
		var deleteAction1 = new DeleteAction(activationSymbol);
		var deleteAction2 = new DeleteAction(newItemSymbol);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol);
		action1.add(activationSymbol);
		action1.add(insertAction);
		action1.add(deleteAction1);
		action1.add(deleteAction2);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(newItemSymbol);
		action2.add(activationSymbol);
		action2.add(insertAction);
		action2.add(deleteAction2);
		action2.add(deleteAction1);
		action2.prepare(model);

		assertFalse(action1.equalsWithSubstitution(action2));
	}

	@Test
	void actionsMixedOrderTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var deleteAction1 = new DeleteAction(newItemSymbol1);

		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol2 = new ActivationVariable();
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
				activationSymbol2);
		var deleteAction2 = new DeleteAction(activationSymbol2);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(newItemSymbol2);
		action1.add(activationSymbol1);
		action1.add(activationSymbol2);
		action1.add(insertAction1);
		action1.add(insertAction2);
		action1.add(deleteAction1);
		action1.add(deleteAction2);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(deleteAction1);
		action2.add(newItemSymbol1);
		action2.add(insertAction1);
		action2.add(newItemSymbol2);
		action2.add(deleteAction2);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.add(activationSymbol2);
		action2.prepare(model);

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void twoUnpreparedActionsTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var deleteAction1 = new DeleteAction(newItemSymbol1);

		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol2 = new ActivationVariable();
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
				activationSymbol2);
		var deleteAction2 = new DeleteAction(activationSymbol2);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(newItemSymbol2);
		action1.add(activationSymbol1);
		action1.add(activationSymbol2);
		action1.add(insertAction1);
		action1.add(insertAction2);
		action1.add(deleteAction1);
		action1.add(deleteAction2);

		var action2 = new TransformationAction();
		action2.add(deleteAction1);
		action2.add(newItemSymbol1);
		action2.add(insertAction1);
		action2.add(newItemSymbol2);
		action2.add(deleteAction2);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.add(activationSymbol2);

		assertTrue(action1.equalsWithSubstitution(action2));
	}

	@Test
	void oneUnpreparedActionTest() {
		var newItemSymbol1 = new NewItemVariable();
		var activationSymbol1 = new ActivationVariable();
		var insertAction1 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol1,
				activationSymbol1);
		var deleteAction1 = new DeleteAction(newItemSymbol1);

		var newItemSymbol2 = new NewItemVariable();
		var activationSymbol2 = new ActivationVariable();
		var insertAction2 = new InsertAction<>(model.getInterpretation(relation1), true, newItemSymbol2,
				activationSymbol2);
		var deleteAction2 = new DeleteAction(activationSymbol2);

		var action1 = new TransformationAction();
		action1.add(newItemSymbol1);
		action1.add(newItemSymbol2);
		action1.add(activationSymbol1);
		action1.add(activationSymbol2);
		action1.add(insertAction1);
		action1.add(insertAction2);
		action1.add(deleteAction1);
		action1.add(deleteAction2);
		action1.prepare(model);

		var action2 = new TransformationAction();
		action2.add(deleteAction1);
		action2.add(newItemSymbol1);
		action2.add(insertAction1);
		action2.add(newItemSymbol2);
		action2.add(deleteAction2);
		action2.add(activationSymbol1);
		action2.add(insertAction2);
		action2.add(activationSymbol2);

		assertFalse(action1.equalsWithSubstitution(action2));
	}
}
