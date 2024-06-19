/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import org.jetbrains.annotations.NotNull;

public interface StatefulAggregate<R, T> {
	void add(T value);

	void remove(T value);

	@NotNull
	R getResult();

	boolean isEmpty();

	StatefulAggregate<R, T> deepCopy();

	default boolean contains(T value) {
		throw new UnsupportedOperationException();
	}
}
