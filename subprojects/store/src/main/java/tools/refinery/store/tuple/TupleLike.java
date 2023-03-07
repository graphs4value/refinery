package tools.refinery.store.tuple;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface TupleLike {
	int getSize();

	int get(int element);

	default int[] toArray() {
		int size = getSize();
		var array = new int[size];
		for (int i = 0; i < size; i++) {
			array[i] = get(i);
		}
		return array;
	}

	default Tuple toTuple() {
		return switch (getSize()) {
			case 0 -> Tuple.of();
			case 1 -> Tuple.of(get(0));
			case 2 -> Tuple.of(get(0), get(1));
			default -> Tuple.of(toArray());
		};
	}

	static String toString(TupleLike tuple) {
		var valuesString = IntStream.range(0, tuple.getSize())
				.mapToObj(i -> Integer.toString(tuple.get(i)))
				.collect(Collectors.joining(", "));
		return "[" + valuesString + "]";
	}
}
