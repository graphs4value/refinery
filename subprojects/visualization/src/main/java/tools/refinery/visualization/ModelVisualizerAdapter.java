package tools.refinery.visualization;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.model.MutableGraph;
import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.visualization.internal.ModelVisualizerBuilderImpl;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();
	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}

	public MutableGraph createVisualizationForCurrentModelState();

	public MutableGraph createVisualizationForModelState(Long version);

	public boolean saveVisualization(MutableGraph graph, String path);

	public boolean saveVisualization(MutableGraph graph, Format format, String path);
}
