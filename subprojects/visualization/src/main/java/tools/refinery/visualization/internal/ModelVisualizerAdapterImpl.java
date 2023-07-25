package tools.refinery.visualization.internal;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import tools.refinery.store.model.Model;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;

import java.io.File;
import java.io.IOException;

import static guru.nidi.graphviz.model.Factory.*;

public class ModelVisualizerAdapterImpl implements ModelVisualizerAdapter {
	private final Model model;
	private final ModelVisualizerStoreAdapter storeAdapter;
	public ModelVisualizerAdapterImpl(Model model, ModelVisualizerStoreAdapter storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ModelVisualizerStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public MutableGraph createVisualizationForCurrentModelState() {
		var interpretations = model.getInterpretations();
		MutableGraph graph = mutGraph("model").setDirected(true);
		for (var entry : interpretations.entrySet()) {
			var key = entry.getKey();
			var arity = key.arity();
			if (arity < 1 || arity > 2) {
				continue;
			}
			var valueType = key.valueType();
			// TODO: support TruthValue
			if (valueType != Boolean.class) {
				continue;
			}
			var cursor = entry.getValue().getAll();
			while (cursor.move()) {
				if (arity == 1) {
					var id = cursor.getKey().get(0);
					graph.add(mutNode(String.valueOf(id)).add("label", key.name() + ": " + id));
				} else {
					var from = cursor.getKey().get(0);
					var to = cursor.getKey().get(1);
					graph.add(mutNode(String.valueOf(from)).addLink(to(mutNode(String.valueOf(to))).with(Label.of(key.name()))));
				}
			}
		}
		return graph;
	}

	@Override
	public MutableGraph createVisualizationForModelState(Long version) {
		var currentVersion = model.getState();
		model.restore(version);
		MutableGraph graph = createVisualizationForCurrentModelState();
		model.restore(currentVersion);
		return graph;
	}

	@Override
	public boolean saveVisualization(MutableGraph graph, String path) {
		return saveVisualization(graph, Format.PNG, path);
	}

	@Override
	public boolean saveVisualization(MutableGraph graph, Format format, String path) {
		try {
			Graphviz.fromGraph(graph).render(format).toFile(new File(path));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
