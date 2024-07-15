/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl;

import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationStoreAdapter;
import tools.refinery.store.dse.propagation.Propagator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

import java.util.List;

// This should not be a record, because we don't want auto-generated {@code equals} and {@code hashCode} methods.
@SuppressWarnings("ClassCanBeRecord")
class PropagationStoreAdapterImpl implements PropagationStoreAdapter {
	private final ModelStore store;
	private final List<Propagator> propagators;
	private final boolean throwOnFatalRejection;

	PropagationStoreAdapterImpl(ModelStore store, List<Propagator> propagators, boolean throwOnFatalRejection) {
		this.store = store;
		this.propagators = propagators;
		this.throwOnFatalRejection = throwOnFatalRejection;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public PropagationAdapter createModelAdapter(Model model) {
		return new PropagationAdapterImpl(model, this);
	}

	List<Propagator> getPropagators() {
		return propagators;
	}

	boolean isThrowOnFatalRejection() {
		return throwOnFatalRejection;
	}
}
