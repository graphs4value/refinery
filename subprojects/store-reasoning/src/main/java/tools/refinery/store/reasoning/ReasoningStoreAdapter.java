/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;

import java.util.Collection;
import java.util.Set;

public interface ReasoningStoreAdapter extends ModelStoreAdapter {
	Collection<AnyPartialSymbol> getPartialSymbols();

	Collection<AnyPartialSymbol> getRefinablePartialSymbols();

	Set<Concreteness> getSupportedInterpretations();

	Model createInitialModel(ModelSeed modelSeed);

	@Override
	ReasoningAdapter createModelAdapter(Model model);
}
