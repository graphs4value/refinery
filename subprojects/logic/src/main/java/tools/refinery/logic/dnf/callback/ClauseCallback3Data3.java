/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf.callback;

import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.DataVariable;

import java.util.Collection;

@FunctionalInterface
public interface ClauseCallback3Data3<T1, T2, T3> {
	Collection<Literal> toLiterals(DataVariable<T1> d1, DataVariable<T2> d2, DataVariable<T3> d3);
}
