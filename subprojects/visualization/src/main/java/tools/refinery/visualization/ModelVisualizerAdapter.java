package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.internal.FileFormat;
import tools.refinery.visualization.internal.ModelVisualizerBuilderImpl;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();
	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}

	public String createDotForCurrentModelState();

	public String createDotForModelState(Long version);

	public boolean saveDot(String dot, String filePath);

	public boolean renderDot(String dot, String filePath);

	public boolean renderDot(String dot, FileFormat format, String filePath);

	public void addTransition(Long from, Long to, String action);


	public void addTransition(Long from, Long to, String action, Tuple  activation);
	public void addSolution(Long state);

	public boolean saveDesignSpace(String path);

	public boolean renderDesignSpace(String path);

	public boolean renderDesignSpace(String path, FileFormat format);
}
