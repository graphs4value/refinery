/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

@FunctionalInterface
public interface BoundPropagator {
	PropagationResult propagateOne();

	default PropagationResult propagateOne(PropagationRequest request) {
		return switch (request) {
			case PROPAGATE -> propagateOne();
			case CONCRETIZE -> concretizeOne();
		};
	}

	default boolean concretizationRequested() {
		return false;
	}

	default PropagationResult concretizeOne() {
		return PropagationResult.UNCHANGED;
	}

	default PropagationResult checkConcretization() {
		return PropagationResult.UNCHANGED;
	}
}
