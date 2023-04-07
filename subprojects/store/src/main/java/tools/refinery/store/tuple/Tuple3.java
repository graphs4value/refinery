package tools.refinery.store.tuple;

import static tools.refinery.store.tuple.TupleConstants.*;

public record Tuple3(int value0, int value1, int value2) implements Tuple {
	@Override
	public int getSize() {
		return 3;
	}

	@Override
	public int get(int element) {
		return switch (element) {
			case 0 -> value0;
			case 1 -> value1;
			case 2 -> value2;
			default -> throw new ArrayIndexOutOfBoundsException(element);
		};
	}

	@Override
	public String toString() {
		return TUPLE_BEGIN + value0 + TUPLE_SEPARATOR + value1 + TUPLE_SEPARATOR + value2 + TUPLE_END;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tuple3 tuple3 = (Tuple3) o;
		return value0 == tuple3.value0 && value1 == tuple3.value1 && value2 == tuple3.value2;
	}

	@Override
	public int hashCode() {
		int hash = 31 + value0;
		hash = 31 * hash + value1;
		hash = 31 * hash + value2;
		return hash;
	}
}
