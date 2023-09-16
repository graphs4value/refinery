/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.update;

import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.interpreter.internal.QueryInterpreterAdapterImpl;
import tools.refinery.store.query.view.TuplePreservingView;
import tools.refinery.store.tuple.Tuple;

public class TuplePreservingViewUpdateListener<T> extends SymbolViewUpdateListener<T> {
	private final TuplePreservingView<T> view;

	TuplePreservingViewUpdateListener(QueryInterpreterAdapterImpl adapter, TuplePreservingView<T> view,
                                      Interpretation<T> interpretation) {
        super(adapter, interpretation);
        this.view = view;
	}

	@Override
	public void put(Tuple key, T fromValue, T toValue, boolean restoring) {
		boolean fromPresent = view.filter(key, fromValue);
		boolean toPresent = view.filter(key, toValue);
		if (fromPresent == toPresent) {
			return;
		}
		var translated = Tuples.flatTupleOf(view.forwardMap(key));
		processUpdate(translated, toPresent);
	}
}
