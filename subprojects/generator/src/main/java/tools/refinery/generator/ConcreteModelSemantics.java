/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Provider;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ConcreteModelSemantics extends ModelSemantics {
	private final Provider<SolutionSerializer> solutionSerializerProvider;

	ConcreteModelSemantics(ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
						   Provider<SolutionSerializer> solutionSerializerProvider) {
		super(problemTrace, store, modelSeed, Concreteness.CANDIDATE);
		this.solutionSerializerProvider = solutionSerializerProvider;
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

	@Override
	public Problem serialize() {
		getPropagationResult().throwIfRejected();
		var serializer = solutionSerializerProvider.get();
		return serializer.serializeSolution(getProblemTrace(), getModel());
	}
}
