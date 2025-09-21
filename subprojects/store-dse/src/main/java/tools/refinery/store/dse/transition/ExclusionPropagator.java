/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.propagation.Propagator;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;

public class ExclusionPropagator implements Propagator {
	@Override
	public BoundPropagator bindToModel(Model model) {
		return new Bound(model, this);
	}

	private static class Bound implements BoundPropagator {
		private static final String EXCLUSION_MESSAGE = "Model excluded by exclusion criteria";
		private static final String ACCEPTANCE_MESSAGE = "Model excluded by acceptance criteria";

		private final Model model;
		private final ExclusionPropagator propagator;
		private ModelQueryAdapter queryEngine;
		private DesignSpaceExplorationAdapter dseAdapter;

		private Bound(Model model, ExclusionPropagator propagator) {
			this.model = model;
			this.propagator = propagator;
		}

		@Override
		public PropagationResult propagateOne() {
			lateBind();
			queryEngine.flushChanges();
			if (dseAdapter.checkExclude()) {
				return new PropagationRejectedResult(propagator, EXCLUSION_MESSAGE);
			}
			return PropagationResult.UNCHANGED;
		}

		@Override
		public PropagationResult checkConcretization() {
			var result = propagateOne();
			if (result.isRejected()) {
				return result;
			}
			if (!dseAdapter.checkAccept()) {
				return new PropagationRejectedResult(propagator, ACCEPTANCE_MESSAGE);
			}
			return PropagationResult.UNCHANGED;
		}

		private void lateBind() {
			if (queryEngine == null) {
				queryEngine = model.getAdapter(ModelQueryAdapter.class);
			}
			if (dseAdapter == null) {
				dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
			}
		}
	}
}
