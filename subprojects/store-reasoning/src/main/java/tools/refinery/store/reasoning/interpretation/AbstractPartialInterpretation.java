/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPartialInterpretation<A extends AbstractValue<A, C>, C>
		implements PartialInterpretation<A, C> {
	private final ReasoningAdapter adapter;
	private final PartialSymbol<A, C> partialSymbol;
	private final Concreteness concreteness;
	private final List<ResultSetListener<A>> listeners = new ArrayList<>();

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

	@Override
	public void addListener(ResultSetListener<A> listener) {
		if (listeners.isEmpty()) {
			startListeningForChanges();
		}
		listeners.add(listener);
	}

	@Override
	public void removeListener(ResultSetListener<A> listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			stopListeningForChanges();
		}
	}

	protected abstract void startListeningForChanges();

	protected abstract void stopListeningForChanges();

	protected void notifyChange(Tuple key, A oldValue, A newValue) {
		int listenerCount = listeners.size();
		// Use a for loop instead of a for-each loop to avoid {@code Iterator} allocation overhead.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < listenerCount; i++) {
			listeners.get(i).put(key, oldValue, newValue);
		}
	}
}
