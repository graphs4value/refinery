/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.modification.DanglingEdges;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.tests.DummyCriterion;
import tools.refinery.store.dse.tests.DummyRandomObjective;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.List;

import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.create;
import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.delete;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.add;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.remove;
import static tools.refinery.store.query.literal.Literals.not;

class CRAExamplesTest {
	private static final Symbol<String> name = Symbol.of("Name", 1, String.class);
	private static final Symbol<Boolean> classElement = Symbol.of("ClassElement", 1);
	private static final Symbol<Boolean> attribute = Symbol.of("Attribute", 1);
	private static final Symbol<Boolean> method = Symbol.of("Method", 1);
	private static final Symbol<Boolean> encapsulates = Symbol.of("Encapsulates", 2);
	private static final Symbol<Boolean> dataDependency = Symbol.of("DataDependency", 2);
	private static final Symbol<Boolean> functionalDependency = Symbol.of("FunctionalDependency", 2);

	private static final AnySymbolView classElementView = new KeyOnlyView<>(classElement);
	private static final AnySymbolView attributeView = new KeyOnlyView<>(attribute);
	private static final AnySymbolView methodView = new KeyOnlyView<>(method);
	private static final AnySymbolView encapsulatesView = new KeyOnlyView<>(encapsulates);

	private static final RelationalQuery feature = Query.of("Feature", (builder, f) -> builder
			.clause(
					attributeView.call(f)
			)
			.clause(
					methodView.call(f)
			));

	private static final RelationalQuery unEncapsulatedFeature = Query.of("unEncapsulatedFeature",
			(builder, f) -> builder.clause(
					feature.call(f),
					not(encapsulatesView.call(Variable.of(), f))
			));

	private static final Rule assignFeatureRule = Rule.of("AssignFeature", (builder, f, c1) -> builder
			.clause(
					feature.call(f),
					classElementView.call(c1),
					not(encapsulatesView.call(Variable.of(), f))
			)
			.action(
					add(encapsulates, c1, f)
			));

	private static final Rule deleteEmptyClassRule = Rule.of("DeleteEmptyClass", (builder, c) -> builder
			.clause(
					classElementView.call(c),
					not(encapsulatesView.call(c, Variable.of()))
			)
			.action(
					remove(classElement, c),
					delete(c, DanglingEdges.IGNORE)
			));

	private static final Rule createClassRule = Rule.of("CreateClass", (builder, f) -> builder
			.clause(
					feature.call(f),
					not(encapsulatesView.call(Variable.of(), f))
			)
			.action((newClass) -> List.of(
					create(newClass),
					add(classElement, newClass),
					add(encapsulates, newClass, f)
			)));

	private static final Rule moveFeatureRule = Rule.of("MoveFeature", (builder, c1, c2, f) -> builder
			.clause(
					classElementView.call(c1),
					classElementView.call(c2),
					c1.notEquivalent(c2),
					feature.call(f),
					encapsulatesView.call(c1, f)
			)
			.action(
					remove(encapsulates, c1, f),
					add(encapsulates, c2, f)
			));

	@Test
	@Disabled("This test is only for debugging purposes")
	void craTest() {
		var store = ModelStore.builder()
				.symbols(classElement, encapsulates, attribute, method, dataDependency, functionalDependency, name)
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
						.transformations(assignFeatureRule, deleteEmptyClassRule, createClassRule, moveFeatureRule)
						.objectives(Objectives.sum(
								new DummyRandomObjective(),
								Objectives.count(unEncapsulatedFeature)
						))
						.accept(Criteria.whenNoMatch(unEncapsulatedFeature))
						.exclude(new DummyCriterion(false)))
				.build();

		var model = store.createEmptyModel();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);

		var nameInterpretation = model.getInterpretation(name);
		var methodInterpretation = model.getInterpretation(method);
		var attributeInterpretation = model.getInterpretation(attribute);
		var dataDependencyInterpretation = model.getInterpretation(dataDependency);
		var functionalDependencyInterpretation = model.getInterpretation(functionalDependency);

		var modificationAdapter = model.getAdapter(ModificationAdapter.class);

		var method1 = modificationAdapter.createObject();
		var method1Id = method1.get(0);
		var method2 = modificationAdapter.createObject();
		var method2Id = method2.get(0);
		var method3 = modificationAdapter.createObject();
		var method3Id = method3.get(0);
		var method4 = modificationAdapter.createObject();
		var method4Id = method4.get(0);
		var attribute1 = modificationAdapter.createObject();
		var attribute1Id = attribute1.get(0);
		var attribute2 = modificationAdapter.createObject();
		var attribute2Id = attribute2.get(0);
		var attribute3 = modificationAdapter.createObject();
		var attribute3Id = attribute3.get(0);
		var attribute4 = modificationAdapter.createObject();
		var attribute4Id = attribute4.get(0);
		var attribute5 = modificationAdapter.createObject();
		var attribute5Id = attribute5.get(0);

		nameInterpretation.put(method1, "M1");
		nameInterpretation.put(method2, "M2");
		nameInterpretation.put(method3, "M3");
		nameInterpretation.put(method4, "M4");
		nameInterpretation.put(attribute1, "A1");
		nameInterpretation.put(attribute2, "A2");
		nameInterpretation.put(attribute3, "A3");
		nameInterpretation.put(attribute4, "A4");
		nameInterpretation.put(attribute5, "A5");

		methodInterpretation.put(method1, true);
		methodInterpretation.put(method2, true);
		methodInterpretation.put(method3, true);
		methodInterpretation.put(method4, true);
		attributeInterpretation.put(attribute1, true);
		attributeInterpretation.put(attribute2, true);
		attributeInterpretation.put(attribute3, true);
		attributeInterpretation.put(attribute4, true);
		attributeInterpretation.put(attribute5, true);

		dataDependencyInterpretation.put(Tuple.of(method1Id, attribute1Id), true);
		dataDependencyInterpretation.put(Tuple.of(method1Id, attribute3Id), true);
		dataDependencyInterpretation.put(Tuple.of(method2Id, attribute2Id), true);
		dataDependencyInterpretation.put(Tuple.of(method3Id, attribute3Id), true);
		dataDependencyInterpretation.put(Tuple.of(method3Id, attribute4Id), true);
		dataDependencyInterpretation.put(Tuple.of(method4Id, attribute3Id), true);
		dataDependencyInterpretation.put(Tuple.of(method4Id, attribute5Id), true);

		functionalDependencyInterpretation.put(Tuple.of(method1Id, attribute3Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method1Id, attribute4Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method2Id, attribute1Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method3Id, attribute1Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method3Id, attribute4Id), true);
		functionalDependencyInterpretation.put(Tuple.of(method4Id, attribute2Id), true);

		var initialVersion = model.commit();
		queryEngine.flushChanges();

		var bestFirst = new BestFirstStoreManager(store, 50);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());
		model.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
	}
}
