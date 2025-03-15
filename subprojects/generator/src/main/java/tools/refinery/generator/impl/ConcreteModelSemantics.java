/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.generator.ModelFacadeResult;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationRejectedException;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Model;

import java.util.Optional;

public class ConcreteModelSemantics extends ConcreteModelFacade implements ModelSemantics {
	public ConcreteModelSemantics(Args args) {
		super(args);
	}

	@Override
	protected ModelFacadeResult afterPropagation(Model model, ModelFacadeResult createInitialModelResult) {
		if (createInitialModelResult.isRejected()) {
			return createInitialModelResult;
		}
		var propagationAdapter = model.getAdapter(PropagationAdapter.class);
		PropagationResult concretizationResult;
		try {
			if (propagationAdapter.concretizationRequested()) {
				concretizationResult = propagationAdapter.concretize();
			} else {
				concretizationResult = propagationAdapter.checkConcretization();
			}
		} catch (PropagationRejectedException e) {
			// Fatal propagation error.
			throw getDiagnostics().wrapPropagationRejectedException(e, getProblemTrace());
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
		throwIfInitializationFailed();
		checkConsistency().throwIfInconsistent();
		return super.serialize();
	}

	@Override
	public Optional<Problem> trySerialize() {
		if (getInitializationResult().isRejected() || !checkConsistency().isConsistent()) {
			return Optional.empty();
		}
		// Skip the check in {@code this.serialize()}.
		return Optional.of(super.serialize());
	}

	@Override
	protected SolutionSerializer getSolutionSerializer() {
		var serializer = super.getSolutionSerializer();
		serializer.setPreserveNewNodes(true);
		return serializer;
	}
}
