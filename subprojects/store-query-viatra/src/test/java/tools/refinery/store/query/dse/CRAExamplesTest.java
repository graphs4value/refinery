package tools.refinery.store.query.dse;

import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.dse.internal.TransformationRule;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

import static tools.refinery.store.query.literal.Literals.not;

public class CRAExamplesTest {
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

	/*Example Transformation rules*/
	private static final RelationalQuery assignFeaturePreconditionHelper = Query.of("AssignFeaturePreconditionHelper",
			(builder, model, c, f) -> builder.clause(
					classElementView.call(c),
					classesView.call(model, c),
					encapsulatesView.call(c, f)
			));

	private static final RelationalQuery assignFeaturePrecondition = Query.of("AssignFeaturePrecondition", (builder, c2, f)
			-> builder.clause((model, c1) -> List.of(
					classModelView.call(model),
					featureView.call(f),
					classElementView.call(c2),
					featuresView.call(model, f),
					classesView.call(model, c1),
					not(assignFeaturePreconditionHelper.call(model, c2, f)),
					not(encapsulatesView.call(c2, f))
			)));

	private static final RelationalQuery deleteEmptyClassPrecondition = Query.of("DeleteEmptyClassPrecondition",
			(builder, model, c) -> builder.clause((f) -> List.of(
					classModelView.call(model),
					classElementView.call(c),
					featuresView.call(model, f),
					not(encapsulatesView.call(c, f))
			)));

	private static final RelationalQuery createClassPreconditionHelper = Query.of("CreateClassPreconditionHelper",
			(builder, model, f, c) -> builder.clause(
					classElementView.call(c),
					classesView.call(model, c),
					encapsulatesView.call(c, f)
			));

	private static final RelationalQuery createClassPrecondition = Query.of("CreateClassPrecondition",
			(builder, model, f) -> builder.clause((c) -> List.of(
					classModelView.call(model),
					featureView.call(f),
					not(createClassPreconditionHelper.call(model, f, c))
			)));

	private static final RelationalQuery moveFeature = Query.of("MoveFeature",
			(builder, c1, c2, f) -> builder.clause((model) -> List.of(
					classModelView.call(model),
					classElementView.call(c1),
					classElementView.call(c2),
					featureView.call(f),
					classesView.call(model, c1),
					classesView.call(model, c2),
					featuresView.call(model, f),
					encapsulatesView.call(c1, f)
			)));

	private static final TransformationRule assignFeatureRule = new TransformationRule("AssignFeature",
			assignFeaturePrecondition,
			(model) -> {
				var isEncapsulatedByInterpretation = model.getInterpretation(isEncapsulatedBy);
				return ((Tuple activation) -> {
					var feature = activation.get(0);
					var classElement = activation.get(1);

					isEncapsulatedByInterpretation.put(Tuple.of(feature, classElement), true);
				});
			});

	private static final TransformationRule deleteEmptyClassRule = new TransformationRule("DeleteEmptyClass",
			deleteEmptyClassPrecondition,
			(model) -> {
				var classesInterpretation = model.getInterpretation(classes);
				var classElementInterpretation = model.getInterpretation(classElement);
				var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
				return ((Tuple activation) -> {
					var modelElement = activation.get(0);
					var classElement = activation.get(1);

					classesInterpretation.put(Tuple.of(modelElement, classElement), false);
					classElementInterpretation.put(Tuple.of(classElement), false);
					dseAdapter.deleteObject(Tuple.of(classElement));
				});
			});

	private static final TransformationRule createClassRule = new TransformationRule("CreateClass",
			createClassPrecondition,
			(model) -> {
				var adapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
				var classElementInterpretation = model.getInterpretation(classElement);
				var classesInterpretation = model.getInterpretation(classes);
				var encapsulatesInterpretation = model.getInterpretation(encapsulates);
				var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
				return ((Tuple activation) -> {
					var modelElement = activation.get(0);
					var feature = activation.get(1);

					var newClassElement = dseAdapter.createObject();
					var newClassElementId = newClassElement.get(0);
					classElementInterpretation.put(newClassElement, true);
					classesInterpretation.put(Tuple.of(modelElement, newClassElementId), true);
					encapsulatesInterpretation.put(Tuple.of(newClassElementId, feature), true);
				});
			});

}
