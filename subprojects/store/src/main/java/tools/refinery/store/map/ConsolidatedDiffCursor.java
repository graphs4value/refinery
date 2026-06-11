/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Objects;

class ConsolidatedDiffCursor<K, V> implements DiffCursor<K, V> {

	private final DiffCursor<K, V> wrappedDiffCursor;
	private DiffEntry<K, V>[] diff;
	private int cursorIndex = -1;

	private record DiffEntry<K, V>(K key, V from, V to) {
	}

	public ConsolidatedDiffCursor(DiffCursor<K, V> diffCursor) {
		wrappedDiffCursor = diffCursor;
	}

	@Override
	public K getKey() {
		return diff != null && 0 <= cursorIndex && cursorIndex < diff.length ? diff[cursorIndex].key : null;
	}

	@Override
	public V getValue() {
		return getToValue();
	}

	@Override
	public boolean isTerminated() {
		return diff != null && cursorIndex >= diff.length;
	}

	@Override
	public boolean move() {
		if (diff == null) {
			consolidate();
		}

		cursorIndex++;
		return cursorIndex < diff.length;
	}

	@Override
	public V getFromValue() {
		return diff != null && 0 <= cursorIndex && cursorIndex < diff.length ? diff[cursorIndex].from : null;
	}

	@Override
	public V getToValue() {
		return diff != null && 0 <= cursorIndex && cursorIndex < diff.length ? diff[cursorIndex].to : null;
	}

	private void consolidate() {
		HashMap<K, AbstractMap.SimpleEntry<V, V>> consolidatedChanges = new HashMap<>();
		while (wrappedDiffCursor.move()) {
			var storedChange = consolidatedChanges.get(wrappedDiffCursor.getKey());
			V fromValue;
			if (storedChange != null) {
				if (!Objects.equals(storedChange.getValue(), wrappedDiffCursor.getFromValue())) {
					throw new IllegalStateException("Inconsistent diff cursor: mismatched previous value and from value");
				}
				fromValue = storedChange.getKey();
			} else {
				fromValue = wrappedDiffCursor.getFromValue();
			}
			V toValue = wrappedDiffCursor.getToValue();

			if (Objects.equals(fromValue, toValue)) {
				consolidatedChanges.remove(wrappedDiffCursor.getKey());
			} else {
				consolidatedChanges.put(wrappedDiffCursor.getKey(), new SimpleEntry<>(fromValue, toValue));
			}
		}

		diff = new DiffEntry[consolidatedChanges.size()];
		int i = 0;
		for (var entry : consolidatedChanges.entrySet()) {
			diff[i] = new DiffEntry<>(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue());
			i++;
		}
	}
}
