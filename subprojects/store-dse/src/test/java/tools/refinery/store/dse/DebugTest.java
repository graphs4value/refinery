/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.tests.DummyRandomCriterion;
import tools.refinery.store.dse.tests.DummyRandomObjective;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.List;

import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.create;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.add;

class DebugTest {
	private static final Symbol<Boolean> classModel = Symbol.of("ClassModel", 1);
	private static final Symbol<Boolean> classElement = Symbol.of("ClassElement", 1);
	private static final Symbol<Boolean> feature = Symbol.of("Feature", 1);
	private static final Symbol<Boolean> features = Symbol.of("Features", 2);
	private static final Symbol<Boolean> classes = Symbol.of("Classes", 2);

	private static final AnySymbolView classModelView = new KeyOnlyView<>(classModel);

	@Test
	@Disabled("This test is only for debugging purposes")
	void BFSTest() {
		var createClassRule = Rule.of("CreateClass", (builder, model) -> builder
				.clause(
						classModelView.call(model)
				)
				.action((newClassElement) -> List.of(
						create(newClassElement),
						add(classElement, newClassElement),
						add(classes, model, newClassElement)
				)));

		var createFeatureRule = Rule.of("CreateFeature", (builder, model) -> builder
				.clause(
						classModelView.call(model)
				)
				.action((newFeature) -> List.of(
						create(newFeature),
						add(feature, newFeature),
						add(features, model, newFeature)
				)));

		var store = ModelStore.builder()
				.symbols(classModel, classElement, feature, classes, features)
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
						.saveStates()
						.saveDesignSpace())
				.with(StateCoderAdapter.builder())
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder()
						.transformations(createClassRule, createFeatureRule)
						.objectives(new DummyRandomObjective())
						.accept(new DummyRandomCriterion())
						.exclude(new DummyRandomCriterion()))
				.build();

		var model = store.createEmptyModel();
		var dseAdapter = model.getAdapter(ModificationAdapter.class);
//		dseAdapter.setRandom(1);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);

		var modelElementInterpretation = model.getInterpretation(classModel);
		var classElementInterpretation = model.getInterpretation(classElement);
		var modelElement = dseAdapter.createObject();
		modelElementInterpretation.put(modelElement, true);
		classElementInterpretation.put(modelElement, true);
		var initialVersion = model.commit();
		queryEngine.flushChanges();

		var bestFirst = new BestFirstStoreManager(store, 50);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());
	}
}
