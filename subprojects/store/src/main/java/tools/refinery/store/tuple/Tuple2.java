/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.tuple;

import static tools.refinery.store.tuple.TupleConstants.*;

public record Tuple2(int value0, int value1) implements Tuple {
	@Override
	public int getSize() {
		return 2;
	}

	@Override
	public int get(int element) {
		return switch (element) {
			case 0 -> value0;
			case 1 -> value1;
			default -> throw new ArrayIndexOutOfBoundsException(element);
		};
	}

	@Override
	public String toString() {
		return TUPLE_BEGIN + value0 + TUPLE_SEPARATOR + value1 + TUPLE_END;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tuple2 tuple2 = (Tuple2) o;
		return value0 == tuple2.value0 && value1 == tuple2.value1;
	}

	@Override
	public int hashCode() {
		int hash = 31 + value0;
		hash = 31 * hash + value1;
		return hash;
	}
}
