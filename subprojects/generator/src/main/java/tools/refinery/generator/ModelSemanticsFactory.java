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
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.util.CancellationToken;

import java.util.Set;

public final class ModelSemanticsFactory {
	@Inject
	private Provider<ModelInitializer> initializerProvider;

	private CancellationToken cancellationToken = CancellationToken.NONE;

	public ModelSemanticsFactory cancellationToken(CancellationToken cancellationToken) {
		this.cancellationToken = cancellationToken;
		return this;
	}

	public ModelSemantics createSemantics(Problem problem) {
		var initializer = initializerProvider.get();
		initializer.readProblem(problem);
		var storeBuilder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(Set.of(Concreteness.PARTIAL)));
		initializer.configureStoreBuilder(storeBuilder);
		var store = storeBuilder.build();
		return new ModelSemantics(initializer.getProblemTrace(), store, initializer.getModelSeed());
	}
}
