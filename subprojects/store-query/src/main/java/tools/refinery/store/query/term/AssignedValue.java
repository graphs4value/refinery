/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import tools.refinery.store.query.literal.Literal;

@FunctionalInterface
public interface AssignedValue<T> {
	Literal toLiteral(DataVariable<T> targetVariable);
}
