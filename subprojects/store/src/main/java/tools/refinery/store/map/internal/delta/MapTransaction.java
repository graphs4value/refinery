/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.delta;

import tools.refinery.store.map.Version;

import java.util.Arrays;
import java.util.Objects;

public record MapTransaction<K, V>(MapDelta<K, V>[] deltas, MapTransaction<K, V> parent, int depth) implements Version {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(deltas);
		result = prime * result + Objects.hash(parent, depth);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		MapTransaction<K, V> other = (MapTransaction<K, V>) obj;
		return depth == other.depth && Objects.equals(parent, other.parent) && Arrays.equals(deltas, other.deltas);
	}

	@Override
	public String toString() {
		return "MapTransaction " + depth + " " + Arrays.toString(deltas);
	}
}
