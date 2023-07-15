/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.store.map.CursorAsIterator;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.dnf.FunctionalDependency;
import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractFunctionView<T> extends SymbolView<T> {
	private final List<Parameter> parameters;

	protected AbstractFunctionView(Symbol<T> symbol, String name, Parameter outParameter) {
		super(symbol, name);
		parameters = createParameters(symbol.arity(), outParameter);
	}

	@Override
	public Set<FunctionalDependency<Integer>> getFunctionalDependencies() {
		var arity = getSymbol().arity();
		var forEach = IntStream.range(0, arity).boxed().collect(Collectors.toUnmodifiableSet());
		var unique = Set.of(arity);
		return Set.of(new FunctionalDependency<>(forEach, unique));
	}

	@Override
	public Set<ViewImplication> getImpliedRelationViews() {
		var symbol = getSymbol();
		var impliedIndices = IntStream.range(0, symbol.arity()).boxed().toList();
		var keysView = new KeyOnlyView<>(symbol);
		return Set.of(new ViewImplication(this, keysView, impliedIndices));
	}

	@Override
	protected boolean doFilter(Tuple key, T value) {
		return true;
	}

	protected Object forwardMapValue(T value) {
		return value;
	}

	protected boolean valueEquals(T value, Object otherForwardMappedValue) {
		return Objects.equals(otherForwardMappedValue, forwardMapValue(value));
	}

	@Override
	public Object[] forwardMap(Tuple key, T value) {
		int size = key.getSize();
		Object[] result = new Object[size + 1];
		for (int i = 0; i < size; i++) {
			result[i] = Tuple.of(key.get(i));
		}
		result[key.getSize()] = forwardMapValue(value);
		return result;
	}

	@Override
	public boolean get(Model model, Object[] tuple) {
		int[] content = new int[tuple.length - 1];
		for (int i = 0; i < tuple.length - 1; i++) {
			if (!(tuple[i] instanceof Tuple1 wrapper)) {
				return false;
			}
			content[i] = wrapper.value0();
		}
		Tuple key = Tuple.of(content);
		var valueInTuple = tuple[tuple.length - 1];
		T valueInMap = model.getInterpretation(getSymbol()).get(key);
		return valueEquals(valueInMap, valueInTuple);
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
		AbstractFunctionView<?> that = (AbstractFunctionView<?>) o;
		return Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), parameters);
	}

	private static List<Parameter> createParameters(int symbolArity, Parameter outParameter) {
		var parameters = new Parameter[symbolArity + 1];
		Arrays.fill(parameters, Parameter.NODE_OUT);
		parameters[symbolArity] = outParameter;
		return List.of(parameters);
	}
}
