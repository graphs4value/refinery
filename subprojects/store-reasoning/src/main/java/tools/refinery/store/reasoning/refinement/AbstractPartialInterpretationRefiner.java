/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;

public abstract class AbstractPartialInterpretationRefiner<A, C> implements PartialInterpretationRefiner<A, C> {
	private final ReasoningAdapter adapter;
	private final PartialSymbol<A, C> partialSymbol;

	protected AbstractPartialInterpretationRefiner(ReasoningAdapter adapter, PartialSymbol<A, C> partialSymbol) {
		this.adapter = adapter;
		this.partialSymbol = partialSymbol;
	}

	@Override
	public ReasoningAdapter getAdapter() {
		return adapter;
	}

	@Override
	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}
}
