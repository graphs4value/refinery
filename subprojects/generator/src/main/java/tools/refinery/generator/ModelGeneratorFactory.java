/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.generator.impl.CancellableCancellationToken;
import tools.refinery.generator.impl.ModelGeneratorImpl;
import tools.refinery.language.model.problem.Problem;
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

import java.util.Collection;
import java.util.Set;

// This class is used as a fluent builder, so it's not necessary to use the return value of all of its methods.
@SuppressWarnings("UnusedReturnValue")
public final class ModelGeneratorFactory extends ModelFacadeFactory<ModelGeneratorFactory> {
	private boolean debugPartialInterpretations;

	private boolean partialInterpretationBasedNeighborhoods;

	private int stateCoderDepth = NeighborhoodCalculator.DEFAULT_DEPTH;

	public ModelGeneratorFactory() {
		keepShadowPredicates(false);
	}

	@Override
	protected ModelGeneratorFactory getSelf() {
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
		var initializer = createModelInitializer();
		initializer.readProblem(problem);
		checkCancelled();
		var cancellationToken = new CancellableCancellationToken(getCancellationToken());
		var storeBuilder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory(getStateCodeCalculatorFactory()))
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(getRequiredInterpretations()));
		initializer.configureStoreBuilder(storeBuilder);
		var store = storeBuilder.build();
		var generator = new ModelGeneratorImpl(initializer.getProblemTrace(), store, initializer.getModelSeed(),
				getSolutionSerializerProvider(), getMetadataCreatorProvider(), cancellationToken,
				isKeepNonExistingObjects());
		generator.getPropagationResult().throwIfRejected();
		return generator;
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		return debugPartialInterpretations || partialInterpretationBasedNeighborhoods ?
				Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE) :
				Set.of(Concreteness.CANDIDATE);
	}

	private StateCodeCalculatorFactory getStateCodeCalculatorFactory() {
		return partialInterpretationBasedNeighborhoods ?
				PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, stateCoderDepth) :
				NeighborhoodCalculator.factory(stateCoderDepth);
	}
}
