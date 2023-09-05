/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

public sealed interface AnyVersionedMap extends Versioned permits VersionedMap {
	long getSize();

	int contentHashCode(ContentHashCode mode);

	boolean contentEquals(AnyVersionedMap other);

	/**
	 * Returns a hash code value for the object.
	 *
	 * @return A hash code value for this object.
	 * @deprecated {@link AnyVersionedMap} instances are mutable, and it is inappropriate to use them as keys in
	 * hash-based collections. Use {@link AnyVersionedMap#contentHashCode(ContentHashCode)} to compute a
	 * <code>hashCode</code> for a {@link AnyVersionedMap} instance according to its contents.
	 */
	@Override
	// This method is mark as @Deprecated to prevent inappropriate use of built-in Java API.
	// Therefore, we don't follow normal procedures for removing deprecated code.
	@SuppressWarnings("squid:S1133")
	@Deprecated(since = "0.0.0")
	int hashCode();

	/**
	 * Compares two objects by reference.
	 *
	 * @param obj The reference object with which to compare.
	 * @return <code>true</code> if this object is the same as the <code>obj</code> argument.
	 * @deprecated {@link AnyVersionedMap} instances are mutable, and it is inappropriate to use them as keys in
	 * hash-based collections. Use {@link AnyVersionedMap#contentEquals(AnyVersionedMap)} to compare two
	 * {@link AnyVersionedMap} instances by their contents.
	 */
	@Override
	// This method is mark as @Deprecated to prevent inappropriate use of built-in Java API.
	// Therefore, we don't follow normal procedures for removing deprecated code.
	@SuppressWarnings("squid:S1133")
	@Deprecated(since = "0.0.0")
	boolean equals(Object obj);

	/**
	 * Checks the integrity of the map, and throws an exception if an inconsistency is detected.
	 */
	void checkIntegrity();
}
