/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

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

	@Override
	protected ModelSemanticsFactory getSelf() {
		return this;
	}

	public ModelSemanticsFactory withCandidateInterpretations(boolean withCandidateInterpretations) {
		this.withCandidateInterpretations = withCandidateInterpretations;
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
		initializer.configureStoreBuilder(storeBuilder, isKeepNonExistingObjects());
		var store = storeBuilder.build();
		return new ModelSemantics(initializer.getProblemTrace(), store, initializer.getModelSeed());
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		return withCandidateInterpretations ? Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE) :
				Set.of(Concreteness.PARTIAL);
	}
}
