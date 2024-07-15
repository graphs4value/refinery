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

import java.util.Set;

public final class ModelSemanticsFactory extends ModelFacadeFactory<ModelSemanticsFactory> {
	@Override
	protected ModelSemanticsFactory getSelf() {
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
						.requiredInterpretations(Set.of(Concreteness.PARTIAL)));
		initializer.configureStoreBuilder(storeBuilder, isKeepNonExistingObjects());
		var store = storeBuilder.build();
		return new ModelSemantics(initializer.getProblemTrace(), store, initializer.getModelSeed());
	}
}
