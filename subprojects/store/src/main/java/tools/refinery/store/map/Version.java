/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

/**
 * Interface denoting versions of {@link Versioned}.
 */
public interface Version {
	/**
	 * Hashcode should be updated in accordance with equals.
	 * @return a hashcode of the object.
	 */
	int hashCode();

	/**
	 * Equivalence of two {@link Version}. This equivalence must satisfy the following constraint (in addition to the
	 * constraints of {@link Object#equals(Object)}: if {@code v1} and {@code v2} are {@link Version}s, and {@code v1
	 * .equals(v2)}, then {@code versioned.restore(v1)} must be {@code equals} to {@code versioned.restore(v2)}.
	 * @param o the other object.
	 * @return weather the two versions are equals.
	 */
	boolean equals(Object o);
}
