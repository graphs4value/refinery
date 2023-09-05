/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.delta;

public record MapDelta<K, V>(K key, V oldValue, V newValue) {
	public K getKey() {
		return key;
	}

	public V getOldValue() {
		return oldValue;
	}

	public V getNewValue() {
		return newValue;
	}
}
