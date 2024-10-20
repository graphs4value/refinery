/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl;

import tools.refinery.store.dse.propagation.*;
import tools.refinery.store.model.Model;

class PropagationAdapterImpl implements PropagationAdapter {
	private final Model model;
	private final PropagationStoreAdapterImpl storeAdapter;
	private final BoundPropagator[] boundPropagators;
	private boolean concretizationInProgress;

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
		return propagate(PropagationRequest.PROPAGATE);
	}

	@Override
	public boolean concretizationRequested() {
		// Use a classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < boundPropagators.length; i++) {
			if (boundPropagators[i].concretizationRequested()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean concretizationInProgress() {
		return concretizationInProgress;
	}

	@Override
	public PropagationResult concretize() {
		PropagationResult result;
		concretizationInProgress = true;
		try {
			result = propagate(PropagationRequest.CONCRETIZE);
		} finally {
			concretizationInProgress = false;
		}
		if (!result.isRejected()) {
			result = result.andThen(checkConcretization());
		}
		return result;
	}

	private PropagationResult propagate(PropagationRequest request) {
		PropagationResult result = PropagationResult.UNCHANGED;
		PropagationResult lastResult;
		do {
			model.checkCancelled();
			lastResult = propagateOne(request);
			result = result.andThen(lastResult);
		} while (lastResult.isChanged());
		if (lastResult instanceof PropagationRejectedResult rejectedResult &&
				rejectedResult.fatal() &&
				storeAdapter.isThrowOnFatalRejection()) {
			rejectedResult.throwIfRejected();
		}
		return result;
	}

	private PropagationResult propagateOne(PropagationRequest request) {
		PropagationResult result = PropagationResult.UNCHANGED;
		for (int i = 0; i < boundPropagators.length; i++) {
			model.checkCancelled();
			var lastResult = propagateUntilFixedPoint(i, request);
			result = result.andThen(lastResult);
			if (result.isRejected()) {
				break;
			}
		}
		return result;
	}

	private PropagationResult propagateUntilFixedPoint(int propagatorIndex, PropagationRequest request) {
		var propagator = boundPropagators[propagatorIndex];
		PropagationResult result = PropagationResult.UNCHANGED;
		PropagationResult lastResult;
		do {
			model.checkCancelled();
			lastResult = propagator.propagateOne(request);
			result = result.andThen(lastResult);
		} while (lastResult.isChanged());
		return result;
	}

	@Override
	public PropagationResult checkConcretization() {
		PropagationResult result = PropagationResult.UNCHANGED;
		// Use a classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < boundPropagators.length; i++) {
			model.checkCancelled();
			var propagator = boundPropagators[i];
			var lastResult = propagator.checkConcretization();
			result = result.andThen(lastResult);
			if (result.isRejected()) {
				break;
			}
		}
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
