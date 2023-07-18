/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.tuple;

import java.util.Arrays;
import java.util.stream.Collectors;

import static tools.refinery.store.tuple.TupleConstants.*;

public final class TupleN implements Tuple {
	private final int[] values;

	TupleN(int[] values) {
		if (values.length < MAX_STATIC_ARITY_TUPLE_SIZE) {
			throw new IllegalArgumentException("Tuples of size at most %d must use static arity Tuple classes"
					.formatted(MAX_STATIC_ARITY_TUPLE_SIZE));
		}
		this.values = Arrays.copyOf(values, values.length);
	}

	@Override
	public int getSize() {
		return values.length;
	}

	@Override
	public int get(int element) {
		return values[element];
	}

	@Override
	public Tuple set(int element, int value) {
		int size = getSize();
		var newValues = new int[size];
		for (int i = 0; i < size; i++) {
			newValues[i] = element == i ? value : values[i];
		}
		return Tuple.of(newValues);
	}

	@Override
	public String toString() {
		var valuesString = Arrays.stream(values)
				.mapToObj(Integer::toString)
				.collect(Collectors.joining(TUPLE_SEPARATOR));
		return TUPLE_BEGIN + valuesString + TUPLE_END;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TupleN other = (TupleN) obj;
		return Arrays.equals(values, other.values);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}
}
