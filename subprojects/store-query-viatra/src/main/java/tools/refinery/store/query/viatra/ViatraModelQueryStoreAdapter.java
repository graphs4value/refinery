/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryStoreAdapter;

public interface ViatraModelQueryStoreAdapter extends ModelQueryStoreAdapter {
	ViatraQueryEngineOptions getEngineOptions();

	@Override
	ViatraModelQueryAdapter createModelAdapter(Model model);
}
