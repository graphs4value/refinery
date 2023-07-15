/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.map.CursorAsIterator;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class TuplePreservingView<T> extends SymbolView<T> {
	private final List<Parameter> parameters;

	protected TuplePreservingView(Symbol<T> symbol, String name) {
		super(symbol, name);
		this.parameters = createParameters(symbol.arity());
	}

	protected TuplePreservingView(Symbol<T> symbol) {
		super(symbol);
		this.parameters = createParameters(symbol.arity());
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
	public boolean canIndexSlot(int slot) {
		return slot >= 0 && slot < getSymbol().arity();
	}

	@Override
	public Iterable<Object[]> getAdjacent(Model model, int slot, Object value) {
		if (!(value instanceof Tuple1 tuple1)) {
			return Set.of();
		}
		return (() -> new CursorAsIterator<>(model.getInterpretation(getSymbol()).getAdjacent(slot, tuple1.get(0)),
				this::forwardMap, this::filter));
	}

	@Override
	public List<Parameter> getParameters() {
		return parameters;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		TuplePreservingView<?> that = (TuplePreservingView<?>) o;
		return Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), parameters);
	}

	private static List<Parameter> createParameters(int arity) {
		var parameters = new Parameter[arity];
		Arrays.fill(parameters, Parameter.NODE_OUT);
		return List.of(parameters);
	}
}
