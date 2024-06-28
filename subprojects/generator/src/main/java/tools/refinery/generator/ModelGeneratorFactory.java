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
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.neighborhood.NeighborhoodCalculator;
import tools.refinery.store.util.CancellationToken;

import java.util.Collection;
import java.util.Set;

// This class is used as a fluent builder, so it's not necessary to use the return value of all of its methods.
@SuppressWarnings("UnusedReturnValue")
public final class ModelGeneratorFactory {
	@Inject
	private Provider<ModelInitializer> initializerProvider;

	@Inject
	private Provider<SolutionSerializer> solutionSerializerProvider;

	private CancellationToken cancellationToken = CancellationToken.NONE;

	private boolean debugPartialInterpretations;

	private boolean partialInterpretationBasedNeighborhoods;

	private int stateCoderDepth = NeighborhoodCalculator.DEFAULT_DEPTH;

	public ModelGeneratorFactory cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return this;
	}

	public ModelGeneratorFactory debugPartialInterpretations(boolean debugPartialInterpretations) {
		this.debugPartialInterpretations = debugPartialInterpretations;
		return this;
	}

	public ModelGeneratorFactory partialInterpretationBasedNeighborhoods(
			boolean partialInterpretationBasedNeighborhoods) {
		this.partialInterpretationBasedNeighborhoods = partialInterpretationBasedNeighborhoods;
		return this;
	}

	public ModelGeneratorFactory stateCoderDepth(int stateCoderDepth) {
		this.stateCoderDepth = stateCoderDepth;
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
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory(getStateCoderCalculatorFactory()))
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(getRequiredInterpretations()));
		initializer.configureStoreBuilder(storeBuilder);
		var store = storeBuilder.build();
		return new ModelGenerator(initializer.getProblemTrace(), store, initializer.getModelSeed(),
				solutionSerializerProvider);
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		return debugPartialInterpretations || partialInterpretationBasedNeighborhoods ?
				Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE) :
				Set.of(Concreteness.CANDIDATE);
	}

	private StateCodeCalculatorFactory getStateCoderCalculatorFactory() {
		return partialInterpretationBasedNeighborhoods ?
				PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, stateCoderDepth) :
				NeighborhoodCalculator.factory(stateCoderDepth);
	}
}
