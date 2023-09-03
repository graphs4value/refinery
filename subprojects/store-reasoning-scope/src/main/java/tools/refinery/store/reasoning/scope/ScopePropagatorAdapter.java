/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.reasoning.refinement.RefinementResult;
import tools.refinery.store.reasoning.scope.internal.ScopePropagatorBuilderImpl;

public interface ScopePropagatorAdapter extends ModelAdapter {
	@Override
	ScopePropagatorStoreAdapter getStoreAdapter();

	RefinementResult propagate();

	static ScopePropagatorBuilder builder() {
		return new ScopePropagatorBuilderImpl();
	}
}
