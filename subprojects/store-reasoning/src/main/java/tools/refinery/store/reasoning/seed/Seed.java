/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.seed;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface Seed<T> {
	int arity();

	Class<T> valueType();

	T majorityValue();

	T get(Tuple key);

	Cursor<Tuple, T> getCursor(T defaultValue, int nodeCount);

	static <T> Builder<T> builder(int arity, Class<T> valueType, T reducedValue) {
		return new Builder<>(arity, valueType, reducedValue);
	}

	static <T> Builder<T> builder(Symbol<T> symbol) {
		return builder(symbol.arity(), symbol.valueType(), symbol.defaultValue());
	}

	static <T> Builder<T> builder(PartialSymbol<T, ?> partialSymbol) {
		return builder(partialSymbol.arity(), partialSymbol.abstractDomain().abstractType(),
				partialSymbol.defaultValue());
	}

	@SuppressWarnings("UnusedReturnValue")
	class Builder<T> {
		private final int arity;
		private final Class<T> valueType;
		private T reducedValue;
		private final Map<Tuple, T> map = new LinkedHashMap<>();

		private Builder(int arity, Class<T> valueType, T reducedValue) {
			this.arity = arity;
			this.valueType = valueType;
			this.reducedValue = reducedValue;
		}

		public Builder<T> reducedValue(T reducedValue) {
			this.reducedValue = reducedValue;
			return this;
		}

		public Builder<T> put(Tuple key, T value) {
			if (key.getSize() != arity) {
				throw new IllegalArgumentException("Expected %s to have %d elements".formatted(key, arity));
			}
			map.put(key, value);
			return this;
		}

		public Builder<T> putAll(Map<Tuple, T> map) {
			for (var entry : map.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
			return this;
		}

		public Seed<T> build() {
			return new MapBasedSeed<>(arity, valueType, reducedValue, Collections.unmodifiableMap(map));
		}
	}
}
