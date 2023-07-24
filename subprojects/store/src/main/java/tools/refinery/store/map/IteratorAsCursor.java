/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class IteratorAsCursor<K, V> implements Cursor<K, V> {
	final Iterator<Entry<K, V>> iterator;
	final VersionedMap<K, V> source;

	private boolean terminated;
	private K key;
	private V value;

	public IteratorAsCursor(VersionedMap<K, V> source, Map<K, V> current) {
		this.iterator = current.entrySet().iterator();
		this.source = source;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public boolean move() {
		terminated = !iterator.hasNext();
		if (terminated) {
			this.key = null;
			this.value = null;
		} else {
			Entry<K, V> next = iterator.next();
			this.key = next.getKey();
			this.value = next.getValue();
		}
		return !terminated;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public Set<AnyVersionedMap> getDependingMaps() {
		return Set.of(this.source);
	}
}
