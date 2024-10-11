/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ConcreteModelSemantics extends ModelSemantics {
	ConcreteModelSemantics(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed) {
		super(problemTrace, store, modelSeed, Concreteness.CANDIDATE);
	}

	@Override
	protected PropagationResult afterPropagation(PropagationResult createInitialModelResult) {
		if (createInitialModelResult.isRejected()) {
			return createInitialModelResult;
		}
		var propagationAdapter = getModel().getAdapter(PropagationAdapter.class);
		if (!propagationAdapter.concretizationRequested()) {
			return createInitialModelResult;
		}
		return propagationAdapter.concretize();
	}
}
