package tools.refinery.store.tuple;

import java.util.Arrays;

public record TupleN(int[] values) implements Tuple {
	static final int CUSTOM_TUPLE_SIZE = 2;

	public TupleN(int[] values) {
		if (values.length < CUSTOM_TUPLE_SIZE)
			throw new IllegalArgumentException();
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
	public int[] toArray() {
		return values;
	}

	@Override
	public String toString() {
		return TupleLike.toString(this);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
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
}
