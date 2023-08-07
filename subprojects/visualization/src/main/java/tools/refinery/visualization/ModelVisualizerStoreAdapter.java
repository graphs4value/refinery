/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.Set;

public interface ModelVisualizerStoreAdapter extends ModelStoreAdapter {

	String getOutputPath();

	boolean isRenderDesignSpace();

	boolean isRenderStates();

	Set<FileFormat> getFormats();
}
