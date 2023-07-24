/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.seed;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

public interface Seed<T> {
	int arity();

	T reducedValue();

	T get(Tuple key);

	Cursor<Tuple, T> getCursor(T defaultValue, int nodeCount);
}
