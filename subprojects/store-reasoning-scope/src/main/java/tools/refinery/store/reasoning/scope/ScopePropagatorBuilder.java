/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;

public interface ScopePropagatorBuilder extends ModelAdapterBuilder {
	ScopePropagatorBuilder countSymbol(Symbol<CardinalityInterval> countSymbol);

	ScopePropagatorBuilder scope(PartialRelation type, CardinalityInterval interval);

	@Override
	ScopePropagatorStoreAdapter build(ModelStore store);
}
