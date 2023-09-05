/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.internal.ModelVisualizerBuilderImpl;

import java.util.Collection;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();
	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}

	public void addTransition(Version from, Version to, String action);


	public void addTransition(Version from, Version to, String action, Tuple  activation);
	public void addState(Version state);
	public void addState(Version state, Collection<Double> fitness);
	public void addState(Version state, String label);
	public void addSolution(Version state);
	public void visualize();

}
