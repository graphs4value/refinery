/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.seed;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.tuple.Tuple;

import java.util.Map;
import java.util.Objects;

record MapBasedSeed<T>(int arity, Class<T> valueType, T majorityValue, Map<Tuple, T> map) implements Seed<T> {
	@Override
	public T get(Tuple key) {
		var value = map.get(key);
		return value == null ? majorityValue : value;
	}

	@Override
	public Cursor<Tuple, T> getCursor(T defaultValue, int nodeCount) {
		if (Objects.equals(defaultValue, majorityValue)) {
			return Cursors.of(map);
		}
		return new CartesianProductCursor<>(arity, nodeCount, majorityValue, defaultValue, map);
	}

	private static class CartesianProductCursor<T> implements Cursor<Tuple, T> {
		private final int nodeCount;
		private final T reducedValue;
		private final T defaultValue;
		private final Map<Tuple, T> map;
		private final int[] counter;
		private State state = State.INITIAL;
		private Tuple key;
		private T value;

		private CartesianProductCursor(int arity, int nodeCount, T reducedValue, T defaultValue, Map<Tuple, T> map) {
			this.nodeCount = nodeCount;
			this.reducedValue = reducedValue;
			this.defaultValue = defaultValue;
			this.map = map;
			counter = new int[arity];
		}

		@Override
		public Tuple getKey() {
			return key;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public boolean isTerminated() {
			return state == State.TERMINATED;
		}

		@Override
		public boolean move() {
			return switch (state) {
				case INITIAL -> {
					state = State.STARTED;
					yield checkValue() || moveToNext();
				}
				case STARTED -> moveToNext();
				case TERMINATED -> false;
			};
		}

		private boolean moveToNext() {
			do {
				increment();
			} while (state != State.TERMINATED && !checkValue());
			return state != State.TERMINATED;
		}

		private void increment() {
			int i = counter.length - 1;
			while (i >= 0) {
				counter[i]++;
				if (counter[i] < nodeCount) {
					return;
				}
				counter[i] = 0;
				i--;
			}
			state = State.TERMINATED;
		}

		private boolean checkValue() {
			key = Tuple.of(counter);
			var valueInMap = map.get(key);
			if (Objects.equals(valueInMap, defaultValue)) {
				return false;
			}
			value = valueInMap == null ? reducedValue : valueInMap;
			return true;
		}

		private enum State {
			INITIAL,
			STARTED,
			TERMINATED
		}
	}
}
