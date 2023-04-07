package tools.refinery.store.tuple;

import static tools.refinery.store.tuple.TupleConstants.TUPLE_BEGIN;
import static tools.refinery.store.tuple.TupleConstants.TUPLE_END;

public final class Tuple0 implements Tuple {
	public static final Tuple0 INSTANCE = new Tuple0();

	private Tuple0() {
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public int get(int element) {
		throw new IndexOutOfBoundsException(element);
	}

	@Override
	public String toString() {
		return TUPLE_BEGIN + TUPLE_END;
	}
}
