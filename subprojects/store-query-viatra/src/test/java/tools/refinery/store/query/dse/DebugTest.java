package tools.refinery.store.query.dse;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dse.internal.TransformationRule;
import tools.refinery.store.query.dse.strategy.DepthFirstStrategy;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;

public class DebugTest {
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
	void BFSTest() {
		var createClassPrecondition = Query.of("CreateClassPrecondition",
				(builder, model) -> builder.clause(
						classModelView.call(model)
				));

		var createClassRule = new TransformationRule("CreateClass",
				createClassPrecondition,
				(model) -> {
					var classesInterpretation = model.getInterpretation(classes);
					var classElementInterpretation = model.getInterpretation(classElement);
					return ((Tuple activation) -> {
						var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
						var modelElement = activation.get(0);

						var newClassElement = dseAdapter.createObject();
						var newClassElementId = newClassElement.get(0);

						classesInterpretation.put(Tuple.of(modelElement, newClassElementId), true);
						classElementInterpretation.put(Tuple.of(newClassElementId), true);
					});
				});

		var createFeaturePrecondition = Query.of("CreateFeaturePrecondition",
				(builder, model) -> builder.clause(
						classModelView.call(model)
				));

		var createFeatureRule = new TransformationRule("CreateFeature",
				createFeaturePrecondition,
				(model) -> {
					var featuresInterpretation = model.getInterpretation(features);
					var featureInterpretation = model.getInterpretation(feature);
					return ((Tuple activation) -> {
						var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
						var modelElement = activation.get(0);

						var newClassElement = dseAdapter.createObject();
						var newClassElementId = newClassElement.get(0);

						featuresInterpretation.put(Tuple.of(modelElement, newClassElementId), true);
						featureInterpretation.put(Tuple.of(newClassElementId), true);
					});
				});

		var store = ModelStore.builder()
				.symbols(classModel, classElement, feature, isEncapsulatedBy, encapsulates, classes, features)
				.with(ViatraModelQueryAdapter.builder()
						.queries(createClassPrecondition, createFeaturePrecondition))
				.with(DesignSpaceExplorationAdapter.builder()
						.transformations(createClassRule, createFeatureRule)
						.strategy(new DepthFirstStrategy(4).continueIfHardObjectivesFulfilled()
//						.strategy(new BestFirstStrategy(4).continueIfHardObjectivesFulfilled()
//								.goOnOnlyIfFitnessIsBetter()
						))
				.with(ModelVisualizerAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);

		var modelElementInterpretation = model.getInterpretation(classModel);
		modelElementInterpretation.put(dseAdapter.createObject(), true);
		queryEngine.flushChanges();


		var states = dseAdapter.explore();
		var visualizer = model.getAdapter(ModelVisualizerAdapter.class);
		for (var state : states) {
			var visualization = visualizer.createVisualizationForModelState(state);
			visualizer.saveVisualization(visualization, "test_output" + state.hashCode() + ".png");
		}
		System.out.println("states size: " + states.size());
		System.out.println("states: " + states);

	}
}
