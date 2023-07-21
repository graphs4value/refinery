package tools.refinery.store.query.dse;

import guru.nidi.graphviz.engine.Format;
import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dse.internal.TransformationRule;
import tools.refinery.store.query.dse.strategy.BestFirstStrategy;
import tools.refinery.store.query.dse.strategy.DepthFirstStrategy;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.query.viatra.tests.QueryAssertions.assertResults;

public class DesignSpaceExplorationTest {
//	private static final Symbol<Boolean> namedElement = Symbol.of("NamedElement", 1);
//	private static final Symbol<Boolean> attribute = Symbol.of("Attribute", 1);
//	private static final Symbol<Boolean> method = Symbol.of("Method", 1);
//	private static final Symbol<Boolean> dataDependency = Symbol.of("DataDependency", 2);
//	private static final Symbol<Boolean> functionalDependency = Symbol.of("FunctionalDependency", 2);

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
	void createObjectTest() {
		var store = ModelStore.builder()
				.with(ViatraModelQueryAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);

		assertEquals(0, dseAdapter.getModelSize());

		var newModel = dseAdapter.createObject();
		var newModelId = newModel.get(0);
		var newClass1 = dseAdapter.createObject();
		var newClass1Id = newClass1.get(0);
		var newClass2 = dseAdapter.createObject();
		var newClass2Id = newClass2.get(0);
		var newField = dseAdapter.createObject();
		var newFieldId = newField.get(0);

		assertEquals(0, newModelId);
		assertEquals(1, newClass1Id);
		assertEquals(2, newClass2Id);
		assertEquals(3, newFieldId);
		assertEquals(4, dseAdapter.getModelSize());
	}

	@Test
	void deleteMiddleObjectTest() {
		var store = ModelStore.builder()
				.with(ViatraModelQueryAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);

		assertEquals(0, dseAdapter.getModelSize());

		var newObject0 = dseAdapter.createObject();
		var newObject0Id = newObject0.get(0);
		var newObject1 = dseAdapter.createObject();
		var newObject1Id = newObject1.get(0);
		var newObject2 = dseAdapter.createObject();
		var newObject2Id = newObject2.get(0);
		var newObject3 = dseAdapter.createObject();
		var newObject3Id = newObject3.get(0);

		assertEquals(0, newObject0Id);
		assertEquals(1, newObject1Id);
		assertEquals(2, newObject2Id);
		assertEquals(3, newObject3Id);
		assertEquals(4, dseAdapter.getModelSize());

		dseAdapter.deleteObject(newObject1);
		assertEquals(4, dseAdapter.getModelSize());

		var newObject4 = dseAdapter.createObject();
		var newObject4Id = newObject4.get(0);
		assertEquals(4, newObject4Id);
		assertEquals(5, dseAdapter.getModelSize());

		dseAdapter.deleteObject(newObject4);
		assertEquals(5, dseAdapter.getModelSize());
	}

}
