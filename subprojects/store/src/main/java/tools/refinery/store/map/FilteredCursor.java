package tools.refinery.store.map;

import java.util.Set;

public abstract class FilteredCursor<K, V> implements Cursor<K, V> {
	private final Cursor<K, V> wrappedCursor;

	protected FilteredCursor(Cursor<K, V> wrappedCursor) {
		this.wrappedCursor = wrappedCursor;
	}

	@Override
	public K getKey() {
		return wrappedCursor.getKey();
	}

	@Override
	public V getValue() {
		return wrappedCursor.getValue();
	}

	@Override
	public boolean isTerminated() {
		return wrappedCursor.isTerminated();
	}

	@Override
	public boolean move() {
		while (wrappedCursor.move()) {
			if (keep()) {
				return true;
			}
		}
		return false;
	}

	protected abstract boolean keep();

	@Override
	public boolean isDirty() {
		return wrappedCursor.isDirty();
	}

	@Override
	public Set<AnyVersionedMap> getDependingMaps() {
		return wrappedCursor.getDependingMaps();
	}
}
