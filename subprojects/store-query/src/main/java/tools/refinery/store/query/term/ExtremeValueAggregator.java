/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class ExtremeValueAggregator<T> implements StatefulAggregator<T, T> {
	private final Class<T> type;
	private final T emptyResult;
	private final Comparator<T> comparator;

	public ExtremeValueAggregator(Class<T> type, T emptyResult) {
		this(type, emptyResult, null);
	}

	public ExtremeValueAggregator(Class<T> type, T emptyResult, Comparator<T> comparator) {
		this.type = type;
		this.emptyResult = emptyResult;
		this.comparator = comparator;
	}

	@Override
	public Class<T> getResultType() {
		return getInputType();
	}

	@Override
	public Class<T> getInputType() {
		return type;
	}

	@Override
	public StatefulAggregate<T, T> createEmptyAggregate() {
		return new Aggregate();
	}

	@Override
	public T getEmptyResult() {
		return emptyResult;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExtremeValueAggregator<?> that = (ExtremeValueAggregator<?>) o;
		return type.equals(that.type) && Objects.equals(emptyResult, that.emptyResult) && Objects.equals(comparator,
				that.comparator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, emptyResult, comparator);
	}

	private class Aggregate implements StatefulAggregate<T, T> {
		private final SortedMap<T, Integer> values;

		private Aggregate() {
			values = new TreeMap<>(comparator);
		}

		private Aggregate(Aggregate other) {
			values = new TreeMap<>(other.values);
		}

		@Override
		public void add(T value) {
			values.compute(value, (ignoredValue, currentCount) -> currentCount == null ? 1 : currentCount + 1);
		}

		@Override
		public void remove(T value) {
			values.compute(value, (theValue, currentCount) -> {
				if (currentCount == null || currentCount <= 0) {
					throw new IllegalStateException("Invalid count %d for value %s".formatted(currentCount, theValue));
				}
				return currentCount.equals(1) ? null : currentCount - 1;
			});
		}

		@Override
		public T getResult() {
			return isEmpty() ? emptyResult : values.firstKey();
		}

		@Override
		public boolean isEmpty() {
			return values.isEmpty();
		}

		@Override
		public StatefulAggregate<T, T> deepCopy() {
			return new Aggregate(this);
		}

		@Override
		public boolean contains(T value) {
			return StatefulAggregate.super.contains(value);
		}
	}
}
