/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

import java.util.Iterator;
import java.util.Map;

public class IteratorBasedCursor<K, V> implements Cursor<K, V> {
	private final Iterator<Map.Entry<K, V>> iterator;
	private Map.Entry<K, V> entry;
	private boolean terminated;

	public IteratorBasedCursor(Iterator<Map.Entry<K, V>> iterator) {
		this.iterator = iterator;
	}

	@Override
	public K getKey() {
		return entry.getKey();
	}

	@Override
	public V getValue() {
		return entry.getValue();
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public boolean move() {
		if (!terminated && iterator.hasNext()) {
			entry = iterator.next();
			return true;
		}
		terminated = true;
		return false;
	}
}
