/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

public interface DiffCursor<K, V> extends Cursor<K,V> {
	public V getFromValue();
	public V getToValue();
}