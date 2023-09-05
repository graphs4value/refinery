/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import java.util.Arrays;

public interface ObjectiveValues {
	public record ObjectiveValue1(double value0) implements ObjectiveValue {
		@Override
		public double get(int index) {
			if(index == 0) return value0;
			else throw new IllegalArgumentException("No value at " + index);
		}

		@Override
		public int getSize() {
			return 1;
		}
	}
	public record ObjectiveValue2(double value0, double value1) implements ObjectiveValue {
		@Override
		public double get(int index) {
			if(index == 0) return value0;
			else if(index == 1) return value1;
			else throw new IllegalArgumentException("No value at " + index);
		}

		@Override
		public int getSize() {
			return 2;
		}
	}
	public record ObjectiveValueN(double[] values) implements ObjectiveValue {
		@Override
		public double get(int index) {
			return values[index];
		}

		@Override
		public int getSize() {
			return values().length;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ObjectiveValueN that = (ObjectiveValueN) o;

			return Arrays.equals(values, that.values);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(values);
		}

		@Override
		public String toString() {
			return "ObjectiveValueN{" +
					"values=" + Arrays.toString(values) +
					'}';
		}
	}
}
