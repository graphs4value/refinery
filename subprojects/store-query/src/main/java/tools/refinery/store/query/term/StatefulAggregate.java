package tools.refinery.store.query.term;

public interface StatefulAggregate<R, T> {
	void add(T value);

	void remove(T value);

	R getResult();

	boolean isEmpty();

	StatefulAggregate<R, T> deepCopy();

	default boolean contains(T value) {
		throw new UnsupportedOperationException();
	}
}
