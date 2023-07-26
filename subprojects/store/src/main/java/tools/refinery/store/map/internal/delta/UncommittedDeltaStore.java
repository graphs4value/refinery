/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.delta;

public interface UncommittedDeltaStore<K, V> {
	void processChange(K key, V oldValue, V newValue);

	MapDelta<K, V>[] extractDeltas();

	MapDelta<K, V>[] extractAndDeleteDeltas();

	default void checkIntegrity() {
		MapDelta<K, V>[] extractedDeltas = extractDeltas();
		if(extractedDeltas != null) {
			for(var uncommittedOldValue : extractedDeltas) {
				if(uncommittedOldValue == null) {
					throw new IllegalArgumentException("Null entry in deltas!");
				}
				if(uncommittedOldValue.getKey() == null) {
					throw new IllegalStateException("Null key in deltas!");
				}
			}
		}
	}

}
