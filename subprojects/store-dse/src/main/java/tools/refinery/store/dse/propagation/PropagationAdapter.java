/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.propagation.impl.PropagationBuilderImpl;

public interface PropagationAdapter extends ModelAdapter {
	@Override
	PropagationStoreAdapter getStoreAdapter();

	PropagationResult propagate();

	static PropagationBuilder builder() {
		return new PropagationBuilderImpl();
	}
}
