/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.language.semantics.SolutionSerializer;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.util.CancellationToken;

import java.util.Collection;
import java.util.Set;

public final class ModelGeneratorFactory {
	@Inject
	private Provider<ModelInitializer> initializerProvider;

	@Inject
	private Provider<SolutionSerializer> solutionSerializerProvider;

	private CancellationToken cancellationToken = CancellationToken.NONE;

	private boolean debugPartialInterpretations;

	public ModelGeneratorFactory cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return this;
	}

	public ModelGeneratorFactory debugPartialInterpretations(boolean debugPartialInterpretations) {
		this.debugPartialInterpretations = debugPartialInterpretations;
		return this;
	}

	public ModelGenerator createGenerator(Problem problem) {
		var initializer = initializerProvider.get();
		initializer.readProblem(problem);
		cancellationToken.checkCancelled();
		var storeBuilder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(getRequiredInterpretations()));
		initializer.configureStoreBuilder(storeBuilder);
		var store = storeBuilder.build();
		return new ModelGenerator(initializer.getProblemTrace(), store, initializer.getModelSeed(),
                solutionSerializerProvider);
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		return debugPartialInterpretations ? Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE) :
				Set.of(Concreteness.CANDIDATE);
	}
}
