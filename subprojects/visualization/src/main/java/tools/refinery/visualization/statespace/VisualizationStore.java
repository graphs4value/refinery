package tools.refinery.visualization.statespace;

import tools.refinery.store.map.Version;

import java.util.Map;

public interface VisualizationStore {
	void addState(Version state, String label);
	void addSolution(Version state);
	void addTransition(Version from, Version to, String label);
	StringBuilder getDesignSpaceStringBuilder();
	Map<Version, Integer> getStates();
}
