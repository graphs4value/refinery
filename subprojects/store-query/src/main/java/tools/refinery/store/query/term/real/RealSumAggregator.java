/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.real;

import tools.refinery.store.query.term.StatefulAggregate;
import tools.refinery.store.query.term.StatefulAggregator;

import java.util.Map;
import java.util.TreeMap;

public final class RealSumAggregator implements StatefulAggregator<Double, Double> {
	public static final RealSumAggregator INSTANCE = new RealSumAggregator();

	private RealSumAggregator() {
	}

	@Override
	public Class<Double> getResultType() {
		return Double.class;
	}

	@Override
	public Class<Double> getInputType() {
		return Double.class;
	}

	@Override
	public StatefulAggregate<Double, Double> createEmptyAggregate() {
		return new Aggregate();
	}

	@Override
	public Double getEmptyResult() {
		return 0d;
	}

	private static class Aggregate implements StatefulAggregate<Double, Double> {
		private final Map<Double, Integer> values;

		public Aggregate() {
			values = new TreeMap<>();
		}

		private Aggregate(Aggregate other) {
			values = new TreeMap<>(other.values);
		}

		@Override
		public void add(Double value) {
			values.compute(value, (ignoredValue, currentCount) -> currentCount == null ? 1 : currentCount + 1);
		}

		@Override
		public void remove(Double value) {
			values.compute(value, (theValue, currentCount) -> {
				if (currentCount == null || currentCount <= 0) {
					throw new IllegalStateException("Invalid count %d for value %f".formatted(currentCount, theValue));
				}
				return currentCount.equals(1) ? null : currentCount - 1;
			});
		}

		@Override
		public Double getResult() {
			return values.entrySet()
					.stream()
					.mapToDouble(entry -> entry.getKey() * entry.getValue())
					.reduce(Double::sum)
					.orElse(0d);
		}

		@Override
		public boolean isEmpty() {
			return values.isEmpty();
		}

		@Override
		public StatefulAggregate<Double, Double> deepCopy() {
			return new Aggregate(this);
		}

		@Override
		public boolean contains(Double value) {
			return values.containsKey(value);
		}
	}
}
