/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;

public interface PropagationStoreAdapter extends ModelStoreAdapter {
	@Override
	PropagationAdapter createModelAdapter(Model model);
}
