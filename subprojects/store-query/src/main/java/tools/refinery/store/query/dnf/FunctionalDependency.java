/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.InvalidQueryException;

import java.util.HashSet;
import java.util.Set;

public record FunctionalDependency<T>(Set<T> forEach, Set<T> unique) {
	public FunctionalDependency {
		var uniqueForEach = new HashSet<>(unique);
		uniqueForEach.retainAll(forEach);
		if (!uniqueForEach.isEmpty()) {
			throw new InvalidQueryException("Variables %s appear on both sides of the functional dependency"
					.formatted(uniqueForEach));
		}
	}
}
