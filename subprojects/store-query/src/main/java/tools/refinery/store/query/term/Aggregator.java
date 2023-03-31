package tools.refinery.store.query.term;

import java.util.stream.Stream;

public interface Aggregator<R, T> {
	Class<R> getResultType();

	Class<T> getInputType();

	R aggregateStream(Stream<T> stream);

	R getEmptyResult();
}
