/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.dse.internal.TransformationRule;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.dse.tests.QueryAssertions.assertResults;

class TransformationRuleTest {

	private static final Symbol<Boolean> classModel = Symbol.of("ClassModel", 1);
	private static final Symbol<Boolean> classElement = Symbol.of("ClassElement", 1);
	private static final Symbol<Boolean> feature = Symbol.of("Feature", 1);

	private static final Symbol<Boolean> isEncapsulatedBy = Symbol.of("IsEncapsulatedBy", 2);
	private static final Symbol<Boolean> encapsulates = Symbol.of("Encapsulates", 2);

	private static final Symbol<Boolean> features = Symbol.of("Features", 2);
	private static final Symbol<Boolean> classes = Symbol.of("Classes", 2);

	private static final AnySymbolView classModelView = new KeyOnlyView<>(classModel);
	private static final AnySymbolView classElementView = new KeyOnlyView<>(classElement);
	private static final AnySymbolView featureView = new KeyOnlyView<>(feature);
	private static final AnySymbolView isEncapsulatedByView = new KeyOnlyView<>(isEncapsulatedBy);
	private static final AnySymbolView encapsulatesView = new KeyOnlyView<>(encapsulates);
	private static final AnySymbolView featuresView = new KeyOnlyView<>(features);
	private static final AnySymbolView classesView = new KeyOnlyView<>(classes);

	@Test
	void activationsTest() {
		var assignFeaturePreconditionHelper = Query.of("AssignFeaturePreconditionHelper",
				(builder, model, c, f) -> builder.clause(
						classElementView.call(c),
						classesView.call(model, c),
						encapsulatesView.call(c, f)
				));

		var assignFeaturePrecondition = Query.of("AssignFeaturePrecondition", (builder, c2, f)
				-> builder.clause((model, c1) -> List.of(
				classModelView.call(model),
				featureView.call(f),
				classElementView.call(c2),
				featuresView.call(model, f),
				classesView.call(model, c1),
				not(assignFeaturePreconditionHelper.call(model, c2, f)),
				not(encapsulatesView.call(c2, f))
		)));

		var deleteEmptyClassPrecondition = Query.of("DeleteEmptyClassPrecondition",
				(builder, model, c) -> builder.clause((f) -> List.of(
						classModelView.call(model),
						classElementView.call(c),
						featuresView.call(model, f),
						not(encapsulatesView.call(c, f))
				)));

		TransformationRule assignFeatureRule = new TransformationRule("AssignFeature",
				assignFeaturePrecondition,
				(model) -> {
					var isEncapsulatedByInterpretation = model.getInterpretation(isEncapsulatedBy);
					return ((Tuple activation) -> {
						var feature = activation.get(0);
						var classElement = activation.get(1);

						isEncapsulatedByInterpretation.put(Tuple.of(feature, classElement), true);
					});
				});

		TransformationRule deleteEmptyClassRule = new TransformationRule("DeleteEmptyClass",
				deleteEmptyClassPrecondition,
				(model) -> {
					var classesInterpretation = model.getInterpretation(classes);
					var classElementInterpretation = model.getInterpretation(classElement);
					return ((Tuple activation) -> {
						var modelElement = activation.get(0);
						var classElement = activation.get(1);

						classesInterpretation.put(Tuple.of(modelElement, classElement), false);
						classElementInterpretation.put(Tuple.of(classElement), false);
					});
				});


		var store = ModelStore.builder()
				.symbols(classModel, classElement, feature, isEncapsulatedBy, encapsulates, classes, features)
				.with(ViatraModelQueryAdapter.builder()
						.queries(assignFeaturePrecondition, assignFeaturePreconditionHelper,
								deleteEmptyClassPrecondition))
				.with(DesignSpaceExplorationAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		assignFeatureRule.prepare(model, queryEngine);
		deleteEmptyClassRule.prepare(model, queryEngine);

		var classModelInterpretation = model.getInterpretation(classModel);
		var classElementInterpretation = model.getInterpretation(classElement);
		var featureInterpretation = model.getInterpretation(feature);
		var featuresInterpretation = model.getInterpretation(features);
		var classesInterpretation = model.getInterpretation(classes);

		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var newModel = dseAdapter.createObject();
		var newModelId = newModel.get(0);
		var newClass1 = dseAdapter.createObject();
		var newClass1Id = newClass1.get(0);
		var newClass2 = dseAdapter.createObject();
		var newClass2Id = newClass2.get(0);
		var newField = dseAdapter.createObject();
		var newFieldId = newField.get(0);

		classModelInterpretation.put(newModel, true);
		classElementInterpretation.put(newClass1, true);
		classElementInterpretation.put(newClass2, true);
		featureInterpretation.put(newField, true);
		classesInterpretation.put(Tuple.of(newModelId, newClass1Id), true);
		classesInterpretation.put(Tuple.of(newModelId, newClass2Id), true);
		featuresInterpretation.put(Tuple.of(newModelId, newFieldId), true);

		queryEngine.flushChanges();

		var assignFeatureRuleActivations = assignFeatureRule.getAllActivationsAsResultSet();
		var deleteEmptyClassRuleActivations = deleteEmptyClassRule.getAllActivationsAsResultSet();

		assertResults(Map.of(
				Tuple.of(newClass1Id, newFieldId), true,
				Tuple.of(newClass2Id, newFieldId), true
		), assignFeatureRuleActivations);

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), true,
				Tuple.of(newModelId, newClass2Id), true
		), deleteEmptyClassRuleActivations);
	}

	@Test
	void randomActivationTest() {
		var deleteEmptyClassPrecondition = Query.of("DeleteEmptyClassPrecondition",
				(builder, model, c) -> builder.clause((f) -> List.of(
						classModelView.call(model),
						classElementView.call(c),
						featuresView.call(model, f),
						not(encapsulatesView.call(c, f))
				)));

		TransformationRule deleteEmptyClassRule0 = new TransformationRule("DeleteEmptyClass0",
				deleteEmptyClassPrecondition,
				(model) -> {
					var classesInterpretation = model.getInterpretation(classes);
					var classElementInterpretation = model.getInterpretation(classElement);
					return ((Tuple activation) -> {
						var modelElement = activation.get(0);
						var classElement = activation.get(1);

						classesInterpretation.put(Tuple.of(modelElement, classElement), false);
						classElementInterpretation.put(Tuple.of(classElement), false);
					});
				},
				0L);

		TransformationRule deleteEmptyClassRule1 = new TransformationRule("DeleteEmptyClass1",
				deleteEmptyClassPrecondition,
				(model) -> {
					var classesInterpretation = model.getInterpretation(classes);
					var classElementInterpretation = model.getInterpretation(classElement);
					return ((Tuple activation) -> {
						var modelElement = activation.get(0);
						var classElement = activation.get(1);

						classesInterpretation.put(Tuple.of(modelElement, classElement), false);
						classElementInterpretation.put(Tuple.of(classElement), false);
					});
				},
				78634L);

		var store = ModelStore.builder()
				.symbols(classModel, classElement, feature, isEncapsulatedBy, encapsulates, classes, features)
				.with(ViatraModelQueryAdapter.builder()
						.queries(deleteEmptyClassPrecondition))
				.with(DesignSpaceExplorationAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		deleteEmptyClassRule0.prepare(model, queryEngine);
		deleteEmptyClassRule1.prepare(model, queryEngine);

		var classModelInterpretation = model.getInterpretation(classModel);
		var classElementInterpretation = model.getInterpretation(classElement);
		var featureInterpretation = model.getInterpretation(feature);
		var featuresInterpretation = model.getInterpretation(features);
		var classesInterpretation = model.getInterpretation(classes);

		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var newModel = dseAdapter.createObject();
		var newModelId = newModel.get(0);
		var newClass1 = dseAdapter.createObject();
		var newClass1Id = newClass1.get(0);
		var newClass2 = dseAdapter.createObject();
		var newClass2Id = newClass2.get(0);
		var newField = dseAdapter.createObject();
		var newFieldId = newField.get(0);

		classModelInterpretation.put(newModel, true);
		classElementInterpretation.put(newClass1, true);
		classElementInterpretation.put(newClass2, true);
		featureInterpretation.put(newField, true);
		classesInterpretation.put(Tuple.of(newModelId, newClass1Id), true);
		classesInterpretation.put(Tuple.of(newModelId, newClass2Id), true);
		featuresInterpretation.put(Tuple.of(newModelId, newFieldId), true);

		queryEngine.flushChanges();


		var activation0 = deleteEmptyClassRule0.getRandomActivation().activation();
		var activation1 = deleteEmptyClassRule1.getRandomActivation().activation();

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), true,
				Tuple.of(newModelId, newClass2Id), true
		), deleteEmptyClassRule0.getAllActivationsAsResultSet());

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), true,
				Tuple.of(newModelId, newClass2Id), true
		), deleteEmptyClassRule1.getAllActivationsAsResultSet());

		assertEquals(Tuple.of(newModelId, newClass2Id), activation0);
		assertEquals(Tuple.of(newModelId, newClass1Id), activation1);

	}

	@Test
	void fireTest() {
		var deleteEmptyClassPrecondition = Query.of("DeleteEmptyClassPrecondition",
				(builder, model, c) -> builder.clause((f) -> List.of(
						classModelView.call(model),
						classElementView.call(c),
						featuresView.call(model, f),
						not(encapsulatesView.call(c, f))
				)));

		TransformationRule deleteEmptyClassRule = new TransformationRule("DeleteEmptyClass",
				deleteEmptyClassPrecondition,
				(model) -> {
					var classesInterpretation = model.getInterpretation(classes);
					var classElementInterpretation = model.getInterpretation(classElement);
					return ((Tuple activation) -> {
						var modelElement = activation.get(0);
						var classElement = activation.get(1);

						classesInterpretation.put(Tuple.of(modelElement, classElement), false);
						classElementInterpretation.put(Tuple.of(classElement), false);
					});
				});

		var store = ModelStore.builder()
				.symbols(classModel, classElement, feature, isEncapsulatedBy, encapsulates, classes, features)
				.with(ViatraModelQueryAdapter.builder()
						.queries(deleteEmptyClassPrecondition))
				.with(DesignSpaceExplorationAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		deleteEmptyClassRule.prepare(model, queryEngine);

		var classModelInterpretation = model.getInterpretation(classModel);
		var classElementInterpretation = model.getInterpretation(classElement);
		var featureInterpretation = model.getInterpretation(feature);
		var featuresInterpretation = model.getInterpretation(features);
		var classesInterpretation = model.getInterpretation(classes);

		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var newModel = dseAdapter.createObject();
		var newModelId = newModel.get(0);
		var newClass1 = dseAdapter.createObject();
		var newClass1Id = newClass1.get(0);
		var newClass2 = dseAdapter.createObject();
		var newClass2Id = newClass2.get(0);
		var newField = dseAdapter.createObject();
		var newFieldId = newField.get(0);

		classModelInterpretation.put(newModel, true);
		classElementInterpretation.put(newClass1, true);
		classElementInterpretation.put(newClass2, true);
		featureInterpretation.put(newField, true);
		classesInterpretation.put(Tuple.of(newModelId, newClass1Id), true);
		classesInterpretation.put(Tuple.of(newModelId, newClass2Id), true);
		featuresInterpretation.put(Tuple.of(newModelId, newFieldId), true);

		queryEngine.flushChanges();

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), true,
				Tuple.of(newModelId, newClass2Id), true
		), deleteEmptyClassRule.getAllActivationsAsResultSet());


		deleteEmptyClassRule.fireActivation(Tuple.of(0, 1));

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), false,
				Tuple.of(newModelId, newClass2Id), true
		), deleteEmptyClassRule.getAllActivationsAsResultSet());
	}

	@Test
	void randomFireTest() {
		var deleteEmptyClassPrecondition = Query.of("DeleteEmptyClassPrecondition",
				(builder, model, c) -> builder.clause((f) -> List.of(
						classModelView.call(model),
						classElementView.call(c),
						featuresView.call(model, f),
						not(encapsulatesView.call(c, f))
				)));

		TransformationRule deleteEmptyClassRule = new TransformationRule("DeleteEmptyClass0",
				deleteEmptyClassPrecondition,
				(model) -> {
					var classesInterpretation = model.getInterpretation(classes);
					var classElementInterpretation = model.getInterpretation(classElement);
					return ((Tuple activation) -> {
						var modelElement = activation.get(0);
						var classElement = activation.get(1);

						classesInterpretation.put(Tuple.of(modelElement, classElement), false);
						classElementInterpretation.put(Tuple.of(classElement), false);
					});
				},
				0L);

		var store = ModelStore.builder()
				.symbols(classModel, classElement, feature, isEncapsulatedBy, encapsulates, classes, features)
				.with(ViatraModelQueryAdapter.builder()
						.queries(deleteEmptyClassPrecondition))
				.with(DesignSpaceExplorationAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		deleteEmptyClassRule.prepare(model, queryEngine);

		var classModelInterpretation = model.getInterpretation(classModel);
		var classElementInterpretation = model.getInterpretation(classElement);
		var featureInterpretation = model.getInterpretation(feature);
		var featuresInterpretation = model.getInterpretation(features);
		var classesInterpretation = model.getInterpretation(classes);

		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var newModel = dseAdapter.createObject();
		var newModelId = newModel.get(0);
		var newClass1 = dseAdapter.createObject();
		var newClass1Id = newClass1.get(0);
		var newClass2 = dseAdapter.createObject();
		var newClass2Id = newClass2.get(0);
		var newField = dseAdapter.createObject();
		var newFieldId = newField.get(0);

		classModelInterpretation.put(newModel, true);
		classElementInterpretation.put(newClass1, true);
		classElementInterpretation.put(newClass2, true);
		featureInterpretation.put(newField, true);
		classesInterpretation.put(Tuple.of(newModelId, newClass1Id), true);
		classesInterpretation.put(Tuple.of(newModelId, newClass2Id), true);
		featuresInterpretation.put(Tuple.of(newModelId, newFieldId), true);

		queryEngine.flushChanges();

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), true,
				Tuple.of(newModelId, newClass2Id), true
		), deleteEmptyClassRule.getAllActivationsAsResultSet());

		deleteEmptyClassRule.fireRandomActivation();

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), true,
				Tuple.of(newModelId, newClass2Id), false
		), deleteEmptyClassRule.getAllActivationsAsResultSet());

		deleteEmptyClassRule.fireRandomActivation();

		assertResults(Map.of(
				Tuple.of(newModelId, newClass1Id), false,
				Tuple.of(newModelId, newClass2Id), false
		), deleteEmptyClassRule.getAllActivationsAsResultSet());

	}
}
