/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.NodeSort;
import tools.refinery.store.query.term.Sort;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;
import tools.refinery.store.representation.Symbol;

import java.util.Arrays;
import java.util.List;

public abstract class TuplePreservingView<T> extends SymbolView<T> {
	protected TuplePreservingView(Symbol<T> symbol, String name) {
		super(symbol, name);
	}

	protected TuplePreservingView(Symbol<T> symbol) {
		super(symbol);
	}

	public Object[] forwardMap(Tuple key) {
		Object[] result = new Object[key.getSize()];
		for (int i = 0; i < key.getSize(); i++) {
			result[i] = Tuple.of(key.get(i));
		}
		return result;
	}

	@Override
	public Object[] forwardMap(Tuple key, T value) {
		return forwardMap(key);
	}

	@Override
	public boolean get(Model model, Object[] tuple) {
		int[] content = new int[tuple.length];
		for (int i = 0; i < tuple.length; i++) {
			if (!(tuple[i] instanceof Tuple1 wrapper)) {
				return false;
			}
			content[i] = wrapper.value0();
		}
		Tuple key = Tuple.of(content);
		T value = model.getInterpretation(getSymbol()).get(key);
		return filter(key, value);
	}

	@Override
	public int arity() {
		return this.getSymbol().arity();
	}

	@Override
	public List<Sort> getSorts() {
		var sorts = new Sort[arity()];
		Arrays.fill(sorts, NodeSort.INSTANCE);
		return List.of(sorts);
	}
}
