/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.translator.AnyPartialSymbolTranslator;
import tools.refinery.store.representation.Symbol;

@SuppressWarnings("UnusedReturnValue")
public interface ReasoningBuilder extends ModelAdapterBuilder {
	ReasoningBuilder initialNodeCount(int nodeCount);

	ReasoningBuilder partialSymbol(AnyPartialSymbolTranslator translator);

	<T> ReasoningBuilder storageRefiner(Symbol<T> symbol, StorageRefiner.Factory<T> refiner);

	ReasoningBuilder initializer(PartialModelInitializer initializer);

	<T> Query<T> lift(Modality modality, Concreteness concreteness, Query<T> query);

	Dnf lift(Modality modality, Concreteness concreteness, Dnf dnf);

	@Override
	ReasoningStoreAdapter build(ModelStore store);
}
