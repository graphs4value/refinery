/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.update;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContextListener;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.InterpretationListener;
import tools.refinery.store.query.interpreter.internal.QueryInterpreterAdapterImpl;
import tools.refinery.store.query.view.SymbolView;
import tools.refinery.store.query.view.TuplePreservingView;

import java.util.ArrayList;
import java.util.List;

public abstract class SymbolViewUpdateListener<T> implements InterpretationListener<T> {
	private final QueryInterpreterAdapterImpl adapter;
	private final Interpretation<T> interpretation;
	private final List<RelationViewFilter> filters = new ArrayList<>();

	protected SymbolViewUpdateListener(QueryInterpreterAdapterImpl adapter, Interpretation<T> interpretation) {
		this.adapter = adapter;
		this.interpretation = interpretation;
	}

	public void addFilter(IInputKey inputKey, ITuple seed, IQueryRuntimeContextListener listener) {
		if (filters.isEmpty()) {
			// First filter to be added, from now on we have to subscribe to model updates.
			interpretation.addListener(this, true);
		}
		filters.add(new RelationViewFilter(inputKey, seed, listener));
	}

	public void removeFilter(IInputKey inputKey, ITuple seed, IQueryRuntimeContextListener listener) {
		if (filters.remove(new RelationViewFilter(inputKey, seed, listener)) && filters.isEmpty()) {
			// Last listener to be added, we don't have be subscribed to model updates anymore.
			interpretation.removeListener(this);
		}
	}

	protected void processUpdate(Tuple tuple, boolean isInsertion) {
		adapter.markAsPending();
		int size = filters.size();
		// Use a for loop instead of a for-each loop to avoid <code>Iterator</code> allocation overhead.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < size; i++) {
			filters.get(i).update(tuple, isInsertion);
		}
	}

	public static <T> SymbolViewUpdateListener<T> of(QueryInterpreterAdapterImpl adapter,
                                                     SymbolView<T> view,
                                                     Interpretation<T> interpretation) {
		if (view instanceof TuplePreservingView<T> tuplePreservingRelationView) {
			return new TuplePreservingViewUpdateListener<>(adapter, tuplePreservingRelationView,
					interpretation);
		}
		return new TupleChangingViewUpdateListener<>(adapter, view, interpretation);
	}
}
