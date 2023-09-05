/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;

import java.util.Set;

public class ModelVisualizeStoreAdapterImpl implements ModelVisualizerStoreAdapter {
	private final ModelStore store;
	private final String outputPath;
	private final boolean renderDesignSpace;
	private final boolean renderStates;
	private final Set<FileFormat> formats;

	public ModelVisualizeStoreAdapterImpl(ModelStore store, String outputPath, Set<FileFormat> formats,
										  boolean renderDesignSpace, boolean renderStates) {
		this.store = store;
		this.outputPath = outputPath;
		this.formats = formats;
		this.renderDesignSpace = renderDesignSpace;
		this.renderStates = renderStates;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public ModelAdapter createModelAdapter(Model model) {
		return new ModelVisualizerAdapterImpl(model, this);
	}

	@Override
	public String getOutputPath() {
		return outputPath;
	}

	@Override
	public boolean isRenderDesignSpace() {
		return renderDesignSpace;
	}

	@Override
	public boolean isRenderStates() {
		return renderStates;
	}

	@Override
	public Set<FileFormat> getFormats() {
		return formats;
	}
}
