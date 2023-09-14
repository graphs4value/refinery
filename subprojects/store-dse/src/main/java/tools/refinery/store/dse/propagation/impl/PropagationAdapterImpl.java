/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl;

import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.propagation.PropagationStoreAdapter;
import tools.refinery.store.model.Model;

class PropagationAdapterImpl implements PropagationAdapter {
	private final Model model;
	private final PropagationStoreAdapterImpl storeAdapter;
	private final BoundPropagator[] boundPropagators;

	public PropagationAdapterImpl(Model model, PropagationStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		var propagators = storeAdapter.getPropagators();
		boundPropagators = new BoundPropagator[propagators.size()];
		for (int i = 0; i < boundPropagators.length; i++) {
			boundPropagators[i] = propagators.get(i).bindToModel(model);
		}
	}

	@Override
	public PropagationResult propagate() {
		PropagationResult result = PropagationResult.UNCHANGED;
		PropagationResult lastResult;
		do {
			model.checkCancelled();
			lastResult = propagateOne();
			result = result.andThen(lastResult);
		} while (lastResult.isChanged());
		return result;
	}

	private PropagationResult propagateOne() {
		PropagationResult result = PropagationResult.UNCHANGED;
		for (int i = 0; i < boundPropagators.length; i++) {
			model.checkCancelled();
			var lastResult = propagateUntilFixedPoint(i);
			result = result.andThen(lastResult);
			if (result.isRejected()) {
				break;
			}
		}
		return result;
	}

	private PropagationResult propagateUntilFixedPoint(int propagatorIndex) {
		var propagator = boundPropagators[propagatorIndex];
		PropagationResult result = PropagationResult.UNCHANGED;
		PropagationResult lastResult;
		do {
			model.checkCancelled();
			lastResult = propagator.propagateOne();
			result = result.andThen(lastResult);
		} while (lastResult.isChanged());
		return result;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public PropagationStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}
}
