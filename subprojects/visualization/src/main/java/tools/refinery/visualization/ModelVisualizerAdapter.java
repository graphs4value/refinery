package tools.refinery.visualization;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.model.MutableGraph;
import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.visualization.internal.FileFormat;
import tools.refinery.visualization.internal.ModelVisualizerBuilderImpl;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();
	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}

	public MutableGraph createVisualizationForCurrentModelState();

	public MutableGraph createVisualizationForModelState(Long version);

	public String createDotForCurrentModelState();

	public String createDotForModelState(Long version);

	public boolean saveVisualization(MutableGraph graph, String path);

	public boolean saveVisualization(MutableGraph graph, Format format, String path);

	public boolean saveDot(String dot, String filePath);

	public boolean renderDot(String dot, String filePath);

	public boolean renderDot(String dot, FileFormat format, String filePath);
}
