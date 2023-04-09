/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import tools.refinery.store.reasoning.internal.ReasoningBuilderImpl;
import tools.refinery.store.adapter.ModelAdapterBuilderFactory;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.representation.PartialRelation;

public final class Reasoning extends ModelAdapterBuilderFactory<ReasoningAdapter,
		ReasoningStoreAdapter, ReasoningBuilder> {
	public static final Reasoning ADAPTER = new Reasoning();

	public static final PartialRelation EXISTS = new PartialRelation("exists", 1);

	public static final PartialRelation EQUALS = new PartialRelation("equals", 1);

	private Reasoning() {
		super(ReasoningAdapter.class, ReasoningStoreAdapter.class, ReasoningBuilder.class);
	}

	@Override
	public ReasoningBuilder createBuilder(ModelStoreBuilder storeBuilder) {
		return new ReasoningBuilderImpl(storeBuilder);
	}
}
