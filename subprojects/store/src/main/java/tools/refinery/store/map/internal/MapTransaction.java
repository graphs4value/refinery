/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal;

import java.util.Arrays;
import java.util.Objects;

public record MapTransaction<K, V>(MapDelta<K, V>[] deltas, long version, MapTransaction<K, V> parent) {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(deltas);
		result = prime * result + Objects.hash(parent, version);
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
		return Arrays.equals(deltas, other.deltas) && Objects.equals(parent, other.parent) && version == other.version;
	}

	@Override
	public String toString() {
		return "MapTransaction " + version + " " + Arrays.toString(deltas);
	}
}
