package tools.refinery.store.map.internal;

import java.util.*;

import tools.refinery.store.map.*;

public class VersionedMapDeltaImpl<K, V> implements VersionedMap<K, V> {
	protected final VersionedMapStoreDeltaImpl<K, V> store;

	final Map<K, V> current;

	final UncommittedDeltaStore<K, V> uncommittedStore;
	MapTransaction<K, V> previous;

	protected final V defaultValue;

	public VersionedMapDeltaImpl(VersionedMapStoreDeltaImpl<K, V> store, V defaultValue) {
		this.store = store;
		this.defaultValue = defaultValue;

		current = new HashMap<>();
		uncommittedStore = new UncommittedDeltaArrayStore<>();
	}

	@Override
	public long commit() {
		MapDelta<K, V>[] deltas = uncommittedStore.extractAndDeleteDeltas();
		long[] versionContainer = new long[1];
		this.previous = this.store.appendTransaction(deltas, previous, versionContainer);
		return versionContainer[0];
	}

	@Override
	public void restore(long state) {
		// 1. restore uncommitted states
		MapDelta<K, V>[] uncommitted = this.uncommittedStore.extractAndDeleteDeltas();
		if (uncommitted != null) {
			backward(uncommitted);
		}

		// 2. get common ancestor
		List<MapDelta<K, V>[]> forward = new ArrayList<>();
		if (this.previous == null) {
			this.store.getPath(state, forward);
			this.forward(forward);
		} else {
			List<MapDelta<K, V>[]> backward = new ArrayList<>();
			this.store.getPath(this.previous.version(), state, backward, forward);
			this.backward(backward);
			this.forward(forward);
		}
	}

	protected void forward(List<MapDelta<K, V>[]> changes) {
		for (int i = changes.size() - 1; i >= 0; i--) {
			forward(changes.get(i));
		}
	}

	protected void backward(List<MapDelta<K, V>[]> changes) {
		for (int i = 0; i < changes.size(); i++) {
			backward(changes.get(i));
		}
	}

	protected void forward(MapDelta<K, V>[] changes) {
		for (int i = 0; i < changes.length; i++) {
			final MapDelta<K, V> change = changes[i];
			current.put(change.getKey(), change.getNewValue());
		}
	}

	protected void backward(MapDelta<K, V>[] changes) {
		for (int i = changes.length - 1; i >= 0; i--) {
			final MapDelta<K, V> change = changes[i];
			current.put(change.getKey(), change.getOldValue());
		}
	}

	@Override
	public V get(K key) {
		return current.getOrDefault(key, defaultValue);
	}

	@Override
	public Cursor<K, V> getAll() {
		return new IteratorAsCursor<>(this, current);
	}

	@Override
	public V put(K key, V value) {
		if (value == defaultValue) {
			V res = current.remove(key);
			if (res == null) {
				// no changes
				return defaultValue;
			} else {
				uncommittedStore.processChange(key, res, value);
				return res;
			}
		} else {
			V oldValue = current.put(key, value);
			uncommittedStore.processChange(key, oldValue, value);
			return oldValue;
		}
	}

	@Override
	public void putAll(Cursor<K, V> cursor) {
		throw new UnsupportedOperationException();

	}

	@Override
	public long getSize() {
		return current.size();
	}

	@Override
	public DiffCursor<K, V> getDiffCursor(long state) {
		MapDelta<K, V>[] backward = this.uncommittedStore.extractDeltas();
		List<MapDelta<K, V>[]> backwardTransactions = new ArrayList<>();
		List<MapDelta<K, V>[]> forwardTransactions = new ArrayList<>();

		if (backward != null) {
			backwardTransactions.add(backward);
		}

		if (this.previous != null) {
			store.getPath(this.previous.version(), state, backwardTransactions, forwardTransactions);
		} else {
			store.getPath(state, forwardTransactions);
		}

		return new DeltaDiffCursor<>(backwardTransactions, forwardTransactions);
	}

	@Override
	public int contentHashCode(ContentHashCode mode) {
		return this.current.hashCode();
	}

	@Override
	public boolean contentEquals(AnyVersionedMap other) {
		if (other instanceof VersionedMapDeltaImpl<?, ?> versioned) {
			if (versioned == this) {
				return true;
			} else {
				return Objects.equals(this.defaultValue, versioned.defaultValue) &&
						Objects.equals(this.current, versioned.current);
			}
		} else {
			throw new UnsupportedOperationException("Comparing different map implementations is ineffective.");
		}
	}
}
