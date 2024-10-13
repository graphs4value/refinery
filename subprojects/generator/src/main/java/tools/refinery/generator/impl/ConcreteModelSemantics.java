/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import com.google.inject.Provider;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ConcreteModelSemantics extends ConcreteModelFacade implements ModelSemantics {
	public ConcreteModelSemantics(
			ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
			Provider<SolutionSerializer> solutionSerializerProvider, boolean keepNonExistingObjects) {
		super(problemTrace, store, modelSeed, solutionSerializerProvider, keepNonExistingObjects);
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
