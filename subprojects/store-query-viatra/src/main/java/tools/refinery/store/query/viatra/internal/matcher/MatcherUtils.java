package tools.refinery.store.query.viatra.internal.matcher;

import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.Iterator;

final class MatcherUtils {
	private MatcherUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static org.eclipse.viatra.query.runtime.matchers.tuple.Tuple toViatraTuple(TupleLike tuple) {
		if (tuple instanceof ViatraTupleLike viatraTupleLike) {
			return viatraTupleLike.wrappedTuple().toImmutable();
		}
		int size = tuple.getSize();
		var array = new Object[size];
		for (int i = 0; i < size; i++) {
			var value = tuple.get(i);
			array[i] = Tuple.of(value);
		}
		return Tuples.flatTupleOf(array);
	}


	public static <T> T getSingleValue(@Nullable Iterable<? extends ITuple> tuples) {
		if (tuples == null) {
			return null;
		}
		return getSingleValue(tuples.iterator());
	}

	public static <T> T getSingleValue(Iterator<? extends ITuple> iterator) {
		if (!iterator.hasNext()) {
			return null;
		}
		var match = iterator.next();
		@SuppressWarnings("unchecked")
		var result = (T) match.get(match.getSize() - 1);
		if (iterator.hasNext()) {
			var input = new OmitOutputViatraTupleLike(match);
			throw new IllegalStateException("Query is not functional for input tuple: " + input);
		}
		return result;
	}
}
