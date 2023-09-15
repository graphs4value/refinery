/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.visualization.ModelVisualizerBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

public class ModelVisualizerBuilderImpl
		extends AbstractModelAdapterBuilder<ModelVisualizerStoreAdapterImpl>
		implements ModelVisualizerBuilder {
	private String dotBinaryPath = "dot";
	private String outputPath;
	private boolean saveDesignSpace = false;
	private boolean saveStates = false;
	private final Set<FileFormat> formats = new LinkedHashSet<>();

	@Override
	protected ModelVisualizerStoreAdapterImpl doBuild(ModelStore store) {
		return new ModelVisualizerStoreAdapterImpl(store, dotBinaryPath, outputPath, formats, saveDesignSpace, saveStates);
	}

	@Override
	public ModelVisualizerBuilder withDotBinaryPath(String dotBinaryPath) {
		checkNotConfigured();
		this.dotBinaryPath = dotBinaryPath;
		return this;
	}

	@Override
	public ModelVisualizerBuilder withOutputPath(String outputPath) {
		checkNotConfigured();
		this.outputPath = outputPath;
		return this;
	}

	@Override
	public ModelVisualizerBuilder withFormat(FileFormat format) {
		checkNotConfigured();
		this.formats.add(format);
		return this;
	}

	@Override
	public ModelVisualizerBuilder saveDesignSpace() {
		checkNotConfigured();
		this.saveDesignSpace = true;
		return this;
	}

	@Override
	public ModelVisualizerBuilder saveStates() {
		checkNotConfigured();
		this.saveStates = true;
		return this;
	}
}
