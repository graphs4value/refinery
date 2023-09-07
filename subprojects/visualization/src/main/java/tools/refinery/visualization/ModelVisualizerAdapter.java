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
import tools.refinery.visualization.statespace.VisualizationStore;

import java.util.Collection;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();
	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}
	void visualize(VisualizationStore visualizationStore);
}
