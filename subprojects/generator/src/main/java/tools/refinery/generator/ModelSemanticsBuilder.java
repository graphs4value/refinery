/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.Set;

public final class ModelSemanticsBuilder extends AbstractRefineryBuilder<ModelSemanticsBuilder> {
	public ModelSemanticsBuilder() {
		super(Set.of(Concreteness.PARTIAL));
	}

	@Override
	protected ModelSemanticsBuilder self() {
		return this;
	}

	public ModelSemantics build() {
		checkProblem();
		var storeBuilder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(getQueryEngineBuilder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(requiredInterpretations));
		initializer.configureStoreBuilder(storeBuilder);
		var store = storeBuilder.build();
		return new ModelSemantics(getProblemTrace(), store, initializer.getModelSeed());
	}
}
