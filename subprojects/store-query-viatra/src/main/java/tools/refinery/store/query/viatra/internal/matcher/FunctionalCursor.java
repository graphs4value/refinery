package tools.refinery.store.query.viatra.internal.matcher;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.rete.index.IterableIndexer;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.TupleLike;

import java.util.Iterator;

class FunctionalCursor<T> implements Cursor<TupleLike, T> {
	private final IterableIndexer indexer;
	private final Iterator<Tuple> iterator;
	private boolean terminated;
	private TupleLike key;
	private T value;

	public FunctionalCursor(IterableIndexer indexer) {
		this.indexer = indexer;
		iterator = indexer.getSignatures().iterator();
	}

	@Override
	public TupleLike getKey() {
		return key;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public boolean move() {
		if (!terminated && iterator.hasNext()) {
			var match = iterator.next();
			key = new ViatraTupleLike(match);
			value = MatcherUtils.getSingleValue(indexer.get(match));
			return true;
		}
		terminated = true;
		return false;
	}
}
