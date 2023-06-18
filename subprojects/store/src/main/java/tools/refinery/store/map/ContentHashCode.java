/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

public enum ContentHashCode {
	/**
	 * Calculate the <code>hashCode</code> of the contents of this {@link AnyVersionedMap} to reduce hash collisions as
	 * much as possible, even iterating over the full contents in necessary.
	 */
	PRECISE_SLOW,

	/**
	 * Compute an approximate <code>hashCode</code> of the contents of this {@link AnyVersionedMap} that may have a
	 * large number of hash collisions, but can be calculated without iterating over the full contents.
	 * <p>
	 *     In the extreme case, {@link AnyVersionedMap#contentHashCode(ContentHashCode)} may return the same
	 *     <code>hashCode</code> irrespectively of the actual contents of the {@link AnyVersionedMap}.
	 * </p>
	 */
	APPROXIMATE_FAST
}
