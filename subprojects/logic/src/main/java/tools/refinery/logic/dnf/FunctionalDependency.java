/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf;

import tools.refinery.logic.InvalidQueryException;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a functional dependency between two sets of variables.
 */
public record FunctionalDependency<T>(Set<T> forEach, Set<T> unique) {
	/**
	 * Validates the functional dependency.
	 *
	 * @throws InvalidQueryException if the functional dependency is invalid
	 */
	public FunctionalDependency {
		//Construct a new HashSet from the unique set.
		var uniqueForEach = new HashSet<>(unique);
		// Check if the unique variables are a subset of the forEach variables by keeping only the common elements in
		// unique for each.
		uniqueForEach.retainAll(forEach);
		// If there are common elements, throw an exception (there shouldn't be any in a functional dependency).
		if (!uniqueForEach.isEmpty()) {
			throw new InvalidQueryException("Variables %s appear on both sides of the functional dependency"
					.formatted(uniqueForEach));
		}
	}
}
