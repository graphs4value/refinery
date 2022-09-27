package tools.refinery.store.tuple;

public sealed interface Tuple extends TupleLike permits Tuple0, Tuple1, Tuple2, TupleN {
	@Override
	default Tuple toTuple() {
		return this;
	}

	static Tuple of() {
		return Tuple0.INSTANCE;
	}

	static Tuple of(int value) {
		return Tuple1.Cache.INSTANCE.getOrCreate(value);
	}

	static Tuple of(int value1, int value2) {
		return new Tuple2(value1, value2);
	}

	static Tuple of(int... values) {
		return switch (values.length) {
			case 0 -> of();
			case 1 -> of(values[0]);
			case 2 -> of(values[0], values[1]);
			default -> new TupleN(values);
		};
	}
}
