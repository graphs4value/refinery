/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.delta;

import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;

import java.util.*;

public class VersionedMapStoreDeltaImpl<K, V> implements VersionedMapStore<K, V> {
	// Configuration
	protected final boolean summarizeChanges;

	// Static data
	protected final V defaultValue;

	public VersionedMapStoreDeltaImpl(boolean summarizeChanges, V defaultValue) {
		this.summarizeChanges = summarizeChanges;
		this.defaultValue = defaultValue;
	}

	@Override
	public VersionedMap<K, V> createMap() {
		return new VersionedMapDeltaImpl<>(this, this.summarizeChanges, this.defaultValue);
	}

	@Override
	public VersionedMap<K, V> createMap(Version state) {
		VersionedMapDeltaImpl<K, V> result = new VersionedMapDeltaImpl<>(this, this.summarizeChanges, this.defaultValue);
		result.restore(state);
		return result;
	}

	public MapTransaction<K, V> appendTransaction(MapDelta<K, V>[] deltas, MapTransaction<K, V> previous) {
		if (deltas == null) {
			return previous;
		} else {
			final int depth;
			if(previous != null) {
				depth = previous.depth()+1;
			} else {
				depth = 0;
			}
			return new MapTransaction<>(deltas, previous, depth);
		}
	}

	@SuppressWarnings("unchecked")
	private MapTransaction<K, V> getState(Version state) {
		return (MapTransaction<K, V>) state;
	}

	public MapTransaction<K, V> getPath(Version to, List<MapDelta<K, V>[]> forwardTransactions) {
		final MapTransaction<K, V> target = getState(to);
		MapTransaction<K, V> toTransaction = target;
		while (toTransaction != null) {
			forwardTransactions.add(toTransaction.deltas());
			toTransaction = toTransaction.parent();
		}
		return target;
	}

	public MapTransaction<K, V> getPath(Version from, Version to,
						List<MapDelta<K, V>[]> backwardTransactions,
						List<MapDelta<K, V>[]> forwardTransactions) {
		MapTransaction<K, V> fromTransaction = getState(from);
		final MapTransaction<K, V> target = getState(to);
		MapTransaction<K, V> toTransaction = target;

		while (fromTransaction != toTransaction) {
			if (fromTransaction == null || (toTransaction != null && fromTransaction.depth() < toTransaction.depth())) {
				forwardTransactions.add(toTransaction.deltas());
				toTransaction = toTransaction.parent();
			} else {
				backwardTransactions.add(fromTransaction.deltas());
				fromTransaction = fromTransaction.parent();
			}
		}
		return target;
	}

	@Override
	public DiffCursor<K, V> getDiffCursor(Version fromState, Version toState) {
		List<MapDelta<K, V>[]> backwardTransactions = new ArrayList<>();
		List<MapDelta<K, V>[]> forwardTransactions = new ArrayList<>();
		getPath(fromState, toState, backwardTransactions, forwardTransactions);
		return new DeltaDiffCursor<>(backwardTransactions, forwardTransactions);
	}
}
