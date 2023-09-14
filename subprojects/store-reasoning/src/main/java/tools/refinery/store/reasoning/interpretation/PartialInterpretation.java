/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public non-sealed interface PartialInterpretation<A, C> extends AnyPartialInterpretation {
	@Override
	PartialSymbol<A, C> getPartialSymbol();

	A get(Tuple key);

	Cursor<Tuple, A> getAll();

	@FunctionalInterface
	interface Factory<A, C> {
		PartialInterpretation<A, C> create(ReasoningAdapter adapter, Concreteness concreteness,
										   PartialSymbol<A, C> partialSymbol);

		default void configure(ModelStoreBuilder storeBuilder, Set<Concreteness> requiredInterpretations) {
			// Nothing to configure by default.
		}
	}
}
