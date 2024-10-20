/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import com.google.inject.Provider;
import tools.refinery.generator.ModelFacadeResult;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.seed.ModelSeed;

public class ConcreteModelSemantics extends ConcreteModelFacade implements ModelSemantics {
	public ConcreteModelSemantics(
			ProblemTrace problemTrace, ModelStore store, ModelSeed modelSeed,
			Provider<SolutionSerializer> solutionSerializerProvider, Provider<MetadataCreator> metadataCreatorProvider,
			boolean keepNonExistingObjects) {
		super(problemTrace, store, modelSeed, solutionSerializerProvider, metadataCreatorProvider,
				keepNonExistingObjects);
	}

	@Override
	protected ModelFacadeResult afterPropagation(ModelFacadeResult createInitialModelResult) {
		if (createInitialModelResult.isRejected()) {
			return createInitialModelResult;
		}
		var propagationAdapter = getModel().getAdapter(PropagationAdapter.class);
		PropagationResult concretizationResult;
		if (propagationAdapter.concretizationRequested()) {
			concretizationResult = propagationAdapter.concretize();
		} else {
			concretizationResult = propagationAdapter.checkConcretization();
		}
		if (concretizationResult instanceof PropagationRejectedResult rejectedResult) {
			return new ModelFacadeResult.ConcretizationRejected(rejectedResult);
		}
		return createInitialModelResult;
	}

	@Override
	protected MetadataCreator getMetadataCreator() {
		var metadataCreator = super.getMetadataCreator();
		metadataCreator.setPreserveNewNodes(true);
		return metadataCreator;
	}


	@Override
	public Problem serialize() {
		// {@link SolutionSerializer} can only serialize consistent models.
		getInitializationResult().throwIfRejected();
		checkConsistency().throwIfInconsistent();
		return super.serialize();
	}

	@Override
	protected SolutionSerializer getSolutionSerializer() {
		var serializer = super.getSolutionSerializer();
		serializer.setPreserveNewNodes(true);
		return serializer;
	}
}
