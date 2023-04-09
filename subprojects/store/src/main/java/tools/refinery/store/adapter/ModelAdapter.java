/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.adapter;

import tools.refinery.store.model.Model;

public interface ModelAdapter {
	Model getModel();

	ModelStoreAdapter getStoreAdapter();
}
