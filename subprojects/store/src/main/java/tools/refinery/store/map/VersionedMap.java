/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

public non-sealed interface VersionedMap<K, V> extends AnyVersionedMap {
	V getDefaultValue();

	V get(K key);

	Cursor<K, V> getAll();

	V put(K key, V value);

	void putAll(Cursor<K, V> cursor);

	DiffCursor<K, V> getDiffCursor(Version state);
}
