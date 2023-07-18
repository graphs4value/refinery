/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.tuple;

import org.jetbrains.annotations.NotNull;

public sealed interface Tuple extends Comparable<Tuple> permits Tuple0, Tuple1, Tuple2, Tuple3, Tuple4, TupleN {
	int getSize();

	int get(int element);

	Tuple set(int element, int value);

	@Override
	default int compareTo(@NotNull Tuple other) {
		int size = getSize();
		int compareSize = Integer.compare(size, other.getSize());
		if (compareSize != 0) {
			return compareSize;
		}
		for (int i = 0; i < size; i++) {
			int compareElement = Integer.compare(get(i), other.get(i));
			if (compareElement != 0) {
				return compareElement;
			}
		}
		return 0;
	}

	static Tuple0 of() {
		return Tuple0.INSTANCE;
	}

	static Tuple1 of(int value) {
		return Tuple1.Cache.INSTANCE.getOrCreate(value);
	}

	static Tuple2 of(int value1, int value2) {
		return new Tuple2(value1, value2);
	}

	static Tuple3 of(int value1, int value2, int value3) {
		return new Tuple3(value1, value2, value3);
	}

	static Tuple4 of(int value1, int value2, int value3, int value4) {
		return new Tuple4(value1, value2, value3, value4);
	}

	static Tuple of(int... values) {
		return switch (values.length) {
			case 0 -> of();
			case 1 -> of(values[0]);
			case 2 -> of(values[0], values[1]);
			case 3 -> of(values[0], values[1], values[2]);
			case 4 -> of(values[0], values[1], values[2], values[3]);
			default -> new TupleN(values);
		};
	}
}
