/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.update;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.query.view.SymbolView;
import tools.refinery.store.tuple.Tuple;

import java.util.Arrays;

public class TupleChangingViewUpdateListener<T> extends SymbolViewUpdateListener<T> {
	private final SymbolView<T> view;

	TupleChangingViewUpdateListener(ViatraModelQueryAdapterImpl adapter, SymbolView<T> view,
									Interpretation<T> interpretation) {
        super(adapter, interpretation);
        this.view = view;
	}

	@Override
	public void put(Tuple key, T fromValue, T toValue, boolean restoring) {
		boolean fromPresent = view.filter(key, fromValue);
		boolean toPresent = view.filter(key, toValue);
		if (fromPresent) {
			if (toPresent) { // value change
				var fromArray = view.forwardMap(key, fromValue);
				var toArray = view.forwardMap(key, toValue);
				if (!Arrays.equals(fromArray, toArray)) {
					processUpdate(Tuples.flatTupleOf(fromArray), false);
					processUpdate(Tuples.flatTupleOf(toArray), true);
				}
			} else { // fromValue disappears
				processUpdate(Tuples.flatTupleOf(view.forwardMap(key, fromValue)), false);
			}
		} else if (toPresent) { // toValue appears
			processUpdate(Tuples.flatTupleOf(view.forwardMap(key, toValue)), true);
		}
	}
}
