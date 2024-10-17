/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.generator.impl.ConcreteModelSemantics;
import tools.refinery.generator.impl.ModelSemanticsImpl;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.Collection;
import java.util.Set;

// This class is used as a fluent builder, so it's not necessary to use the return value of all of its methods.
@SuppressWarnings("UnusedReturnValue")
public final class ModelSemanticsFactory extends ModelFacadeFactory<ModelSemanticsFactory> {
	private boolean withCandidateInterpretations;
	private boolean concretize;

	@Override
	protected ModelSemanticsFactory getSelf() {
		return this;
	}

	public ModelSemanticsFactory withCandidateInterpretations(boolean withCandidateInterpretations) {
		this.withCandidateInterpretations = withCandidateInterpretations;
		return this;
	}

	public ModelSemanticsFactory concretize(boolean concretize) {
		this.concretize = concretize;
		return this;
	}

	public ModelSemantics createSemantics(Problem problem) {
		var semantics = tryCreateSemantics(problem);
		semantics.getPropagationResult().throwIfRejected();
		return semantics;
	}

	public ModelSemantics tryCreateSemantics(Problem problem) {
		var initializer = createModelInitializer();
		initializer.readProblem(problem);
		checkCancelled();
		var storeBuilder = ModelStore.builder()
				.cancellationToken(getCancellationToken())
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder()
						.throwOnFatalRejection(false))
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(getRequiredInterpretations()));
		initializer.configureStoreBuilder(storeBuilder);
		var store = storeBuilder.build();
		var problemTrace = initializer.getProblemTrace();
		var modelSeed = initializer.getModelSeed();
		if (concretize) {
			return new ConcreteModelSemantics(problemTrace, store, modelSeed, getSolutionSerializerProvider(),
					getMetadataCreatorProvider(), isKeepNonExistingObjects());
		}
		return new ModelSemanticsImpl(problemTrace, store, modelSeed, getMetadataCreatorProvider());
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		if (concretize) {
			return Set.of(Concreteness.CANDIDATE);
		}
		if (withCandidateInterpretations) {
			return Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE);
		}
		return Set.of(Concreteness.PARTIAL);
	}
}
