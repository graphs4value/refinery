/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.tuple;

import org.jetbrains.annotations.NotNull;

import static tools.refinery.store.tuple.TupleConstants.*;

public record Tuple4(int value0, int value1, int value2, int value3) implements Tuple {
	@Override
	public int getSize() {
		return 4;
	}

	@Override
	public int get(int element) {
		return switch (element) {
			case 0 -> value0;
			case 1 -> value1;
			case 2 -> value2;
			case 3 -> value3;
			default -> throw new ArrayIndexOutOfBoundsException(element);
		};
	}

	@Override
	public Tuple set(int element, int value) {
		return switch (element) {
			case 0 -> Tuple.of(value, value1, value2, value3);
			case 1 -> Tuple.of(value0, value, value2, value3);
			case 2 -> Tuple.of(value0, value1, value, value3);
			case 3 -> Tuple.of(value0, value1, value2, value);
			default -> throw new ArrayIndexOutOfBoundsException(element);
		};
	}

	@Override
	public String toString() {
		return TUPLE_BEGIN + value0 + TUPLE_SEPARATOR + value1 + TUPLE_SEPARATOR + value2 + TUPLE_SEPARATOR + value3 +
				TUPLE_END;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tuple4 tuple4 = (Tuple4) o;
		return value0 == tuple4.value0 && value1 == tuple4.value1 && value2 == tuple4.value2 && value3 == tuple4.value3;
	}

	@Override
	public int hashCode() {
		int hash = 31 + value0;
		hash = 31 * hash + value1;
		hash = 31 * hash + value2;
		hash = 31 * hash + value3;
		return hash;
	}

	@Override
	public int compareTo(@NotNull Tuple other) {
		if (other instanceof Tuple4 other4) {
			int compare0 = Integer.compare(value0, other4.value0);
			if (compare0 != 0) {
				return compare0;
			}
			int compare1 = Integer.compare(value1, other4.value1);
			if (compare1 != 0) {
				return compare1;
			}
			int compare2 = Integer.compare(value2, other4.value2);
			if (compare2 != 0) {
				return compare2;
			}
			return Integer.compare(value3, other4.value3);
		}
		return Tuple.super.compareTo(other);
	}
}
