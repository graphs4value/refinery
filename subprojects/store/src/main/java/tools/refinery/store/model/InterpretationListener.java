/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model;

import tools.refinery.store.tuple.Tuple;

public interface InterpretationListener<T> {
	void put(Tuple key, T fromValue, T toValue, boolean restoring);
}
