/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;

public abstract class AbstractPartialInterpretation<A, C> implements PartialInterpretation<A, C> {
	private final ReasoningAdapter adapter;
	private final PartialSymbol<A, C> partialSymbol;
	private final Concreteness concreteness;

	protected AbstractPartialInterpretation(ReasoningAdapter adapter, Concreteness concreteness,
											PartialSymbol<A, C> partialSymbol) {
		this.adapter = adapter;
		this.partialSymbol = partialSymbol;
		this.concreteness = concreteness;
	}

	@Override
	public ReasoningAdapter getAdapter() {
		return adapter;
	}

	@Override
	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}

	@Override
	public Concreteness getConcreteness() {
		return concreteness;
	}
}
