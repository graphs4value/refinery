package tools.refinery.store.dse;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.dse.internal.TransformationRule;
import tools.refinery.store.dse.strategy.BestFirstStrategy;
import tools.refinery.store.dse.strategy.DepthFirstStrategy;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.List;

import static tools.refinery.store.query.literal.Literals.not;

public class CRAExamplesTest {
	private static final Symbol<String> name = Symbol.of("Name", 1, String.class);

//	private static final Symbol<Boolean> classModel = Symbol.of("ClassModel", 1);
	private static final Symbol<Boolean> classElement = Symbol.of("ClassElement", 1);
//	private static final Symbol<Boolean> feature = Symbol.of("Feature", 1);
	private static final Symbol<Boolean> attribute = Symbol.of("Attribute", 1);
	private static final Symbol<Boolean> method = Symbol.of("Method", 1);

//	private static final Symbol<Boolean> isEncapsulatedBy = Symbol.of("IsEncapsulatedBy", 2);
	private static final Symbol<Boolean> encapsulates = Symbol.of("Encapsulates", 2);
	private static final Symbol<Boolean> dataDependency = Symbol.of("DataDependency", 2);
	private static final Symbol<Boolean> functionalDependency = Symbol.of("FunctionalDependency", 2);

	private static final Symbol<Boolean> features = Symbol.of("Features", 2);
	private static final Symbol<Boolean> classes = Symbol.of("Classes", 2);

//	private static final AnySymbolView classModelView = new KeyOnlyView<>(classModel);
	private static final AnySymbolView classElementView = new KeyOnlyView<>(classElement);
//	private static final AnySymbolView featureView = new KeyOnlyView<>(feature);
	private static final AnySymbolView attributeView = new KeyOnlyView<>(attribute);
	private static final AnySymbolView methodView = new KeyOnlyView<>(method);
//	private static final AnySymbolView isEncapsulatedByView = new KeyOnlyView<>(isEncapsulatedBy);
	private static final AnySymbolView encapsulatesView = new KeyOnlyView<>(encapsulates);
	private static final AnySymbolView dataDependencyView = new KeyOnlyView<>(dataDependency);
	private static final AnySymbolView functionalDependencyView = new KeyOnlyView<>(functionalDependency);
	private static final AnySymbolView featuresView = new KeyOnlyView<>(features);
	private static final AnySymbolView classesView = new KeyOnlyView<>(classes);

	/*Example Transformation rules*/
	private static final RelationalQuery feature = Query.of("Feature",
			(builder, f) -> builder.clause(
				attributeView.call(f))
			.clause(
				methodView.call(f))
			);

	private static final RelationalQuery assignFeaturePreconditionHelper = Query.of("AssignFeaturePreconditionHelper",
			(builder, c, f) -> builder.clause(
					classElementView.call(c),
//					classesView.call(model, c),
					encapsulatesView.call(c, f)
			));

	private static final RelationalQuery assignFeaturePrecondition = Query.of("AssignFeaturePrecondition",
			(builder, f, c1) -> builder.clause((c2) -> List.of(
//					classModelView.call(model),
					feature.call(f),
					classElementView.call(c1),
//					featuresView.call(model, f),
					not(assignFeaturePreconditionHelper.call(c2, f)),
					not(encapsulatesView.call(c1, f))
			)));

	private static final RelationalQuery deleteEmptyClassPrecondition = Query.of("DeleteEmptyClassPrecondition",
			(builder, c) -> builder.clause((f) -> List.of(
//					classModelView.call(model),
					classElementView.call(c),
//					featuresView.call(model, f),
					not(encapsulatesView.call(c, f))
			)));

	private static final RelationalQuery createClassPreconditionHelper = Query.of("CreateClassPreconditionHelper",
			(builder, f, c) -> builder.clause(
					classElementView.call(c),
//					classesView.call(model, c),
					encapsulatesView.call(c, f)
			));

	private static final RelationalQuery createClassPrecondition = Query.of("CreateClassPrecondition",
			(builder, f) -> builder.clause((c) -> List.of(
//					classModelView.call(model),
					feature.call(f),
					not(createClassPreconditionHelper.call(f, c))
			)));

	private static final RelationalQuery moveFeaturePrecondition = Query.of("MoveFeature",
			(builder, c1, c2, f) -> builder.clause(
//					classModelView.call(model),
					classElementView.call(c1),
					classElementView.call(c2),
					c1.notEquivalent(c2),
					feature.call(f),
//					classesView.call(model, c1),
//					classesView.call(model, c2),
//					featuresView.call(model, f),
					encapsulatesView.call(c1, f)
			));

	private static final TransformationRule assignFeatureRule = new TransformationRule("AssignFeature",
			assignFeaturePrecondition,
			(model) -> {
//				var isEncapsulatedByInterpretation = model.getInterpretation(isEncapsulatedBy);
				var encapsulatesInterpretation = model.getInterpretation(encapsulates);
				return ((Tuple activation) -> {
					var feature = activation.get(0);
					var classElement = activation.get(1);

//					isEncapsulatedByInterpretation.put(Tuple.of(feature, classElement), true);
					encapsulatesInterpretation.put(Tuple.of(classElement, feature), true);
				});
			});

	private static final TransformationRule deleteEmptyClassRule = new TransformationRule("DeleteEmptyClass",
			deleteEmptyClassPrecondition,
			(model) -> {
//				var classesInterpretation = model.getInterpretation(classes);
				var classElementInterpretation = model.getInterpretation(classElement);
				return ((Tuple activation) -> {
					// TODO: can we move dseAdapter outside?
					var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
//					var modelElement = activation.get(0);
					var classElement = activation.get(0);

//					classesInterpretation.put(Tuple.of(modelElement, classElement), false);
					classElementInterpretation.put(Tuple.of(classElement), false);
					dseAdapter.deleteObject(Tuple.of(classElement));
				});
			});

	private static final TransformationRule createClassRule = new TransformationRule("CreateClass",
			createClassPrecondition,
			(model) -> {
				var classElementInterpretation = model.getInterpretation(classElement);
//				var classesInterpretation = model.getInterpretation(classes);
				var encapsulatesInterpretation = model.getInterpretation(encapsulates);
				return ((Tuple activation) -> {
					// TODO: can we move dseAdapter outside?
					var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
//					var modelElement = activation.get(0);
					var feature = activation.get(0);

					var newClassElement = dseAdapter.createObject();
					var newClassElementId = newClassElement.get(0);
					classElementInterpretation.put(newClassElement, true);
//					classesInterpretation.put(Tuple.of(modelElement, newClassElementId), true);
					encapsulatesInterpretation.put(Tuple.of(newClassElementId, feature), true);
				});
			});

	private static final TransformationRule moveFeatureRule = new TransformationRule("MoveFeature",
			moveFeaturePrecondition,
			(model) -> {
				var encapsulatesInterpretation = model.getInterpretation(encapsulates);
				return ((Tuple activation) -> {
					var classElement1 = activation.get(0);
					var classElement2 = activation.get(1);
					var feature = activation.get(2);

					encapsulatesInterpretation.put(Tuple.of(classElement1, feature), false);
					encapsulatesInterpretation.put(Tuple.of(classElement2, feature), true);
				});
			});

	@Test
	void craTest() {
		var store = ModelStore.builder()
				.symbols(classElement, encapsulates, classes, features, attribute, method, dataDependency,
						functionalDependency, name)
				.with(ViatraModelQueryAdapter.builder()
						.queries(feature, assignFeaturePreconditionHelper, assignFeaturePrecondition,
								deleteEmptyClassPrecondition, createClassPreconditionHelper, createClassPrecondition,
								moveFeaturePrecondition))
				.with(ModelVisualizerAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder()
						.transformations(assignFeatureRule, deleteEmptyClassRule, createClassRule, moveFeatureRule)
//						.strategy(new DepthFirstStrategy(3).continueIfHardObjectivesFulfilled()
						.strategy(new BestFirstStrategy(6).continueIfHardObjectivesFulfilled()
//								.goOnOnlyIfFitnessIsBetter()
						))
				.build();

		var model = store.createEmptyModel();
		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
//		dseAdapter.setRandom(1);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);

//		var modelInterpretation = model.getInterpretation(classModel);
		var nameInterpretation = model.getInterpretation(name);
		var methodInterpretation = model.getInterpretation(method);
		var attributeInterpretation = model.getInterpretation(attribute);
		var dataDependencyInterpretation = model.getInterpretation(dataDependency);
		var functionalDependencyInterpretation = model.getInterpretation(functionalDependency);

//		var modelElement = dseAdapter.createObject();
		var method1 = dseAdapter.createObject();
		var method1Id = method1.get(0);
		var method2 = dseAdapter.createObject();
		var method2Id = method2.get(0);
		var method3 = dseAdapter.createObject();
		var method3Id = method3.get(0);
		var method4 = dseAdapter.createObject();
		var method4Id = method4.get(0);
		var attribute1 = dseAdapter.createObject();
		var attribute1Id = attribute1.get(0);
		var attribute2 = dseAdapter.createObject();
		var attribute2Id = attribute2.get(0);
		var attribute3 = dseAdapter.createObject();
		var attribute3Id = attribute3.get(0);
		var attribute4 = dseAdapter.createObject();
		var attribute4Id = attribute4.get(0);
		var attribute5 = dseAdapter.createObject();
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



//		modelInterpretation.put(modelElement, true);
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

		queryEngine.flushChanges();

		var states = dseAdapter.explore();
		System.out.println("states size: " + states.size());
		System.out.println("states: " + states);
		var visualizer = model.getAdapter(ModelVisualizerAdapter.class);
		visualizer.renderDesignSpace("test_output", FileFormat.SVG);
	}

}
