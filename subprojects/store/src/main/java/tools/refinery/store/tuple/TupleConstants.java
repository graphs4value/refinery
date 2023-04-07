package tools.refinery.store.tuple;

final class TupleConstants {
	public static final int MAX_STATIC_ARITY_TUPLE_SIZE = 4;
	public static final String TUPLE_BEGIN = "[";
	public static final String TUPLE_SEPARATOR = ", ";
	public static final String TUPLE_END = "]";

	private TupleConstants() {
		throw new IllegalArgumentException("This is a static utility class an should not instantiated directly");
	}
}
