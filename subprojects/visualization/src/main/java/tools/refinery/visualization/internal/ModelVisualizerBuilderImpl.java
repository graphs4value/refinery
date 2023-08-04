/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.visualization.ModelVisualizerBuilder;

public class ModelVisualizerBuilderImpl
		extends AbstractModelAdapterBuilder<ModelVisualizeStoreAdapterImpl>
		implements ModelVisualizerBuilder {
	@Override
	protected ModelVisualizeStoreAdapterImpl doBuild(ModelStore store) {
		return new ModelVisualizeStoreAdapterImpl(store);
	}
}
