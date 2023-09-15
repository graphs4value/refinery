/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.visualization.internal.FileFormat;

public interface ModelVisualizerBuilder extends ModelAdapterBuilder {
	ModelVisualizerBuilder withDotBinaryPath(String dotBinaryPath);
	ModelVisualizerBuilder withOutputPath(String outputPath);
	ModelVisualizerBuilder withFormat(FileFormat format);
	ModelVisualizerBuilder saveDesignSpace();
	ModelVisualizerBuilder saveStates();
}
