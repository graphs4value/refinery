package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import tools.refinery.store.tuple.Tuple1;
import tools.refinery.store.tuple.TupleLike;

public record ViatraTupleLike(ITuple wrappedTuple) implements TupleLike {
	@Override
	public int getSize() {
		return wrappedTuple.getSize();
	}

	@Override
	public int get(int element) {
		var wrappedValue = (Tuple1) wrappedTuple.get(element);
		return wrappedValue.value0();
	}
}
