/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf.callback;

import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;

import java.util.Collection;

@FunctionalInterface
public interface ClauseCallback3Data2<T1, T2> {
	Collection<Literal> toLiterals(NodeVariable v1, DataVariable<T1> d1, DataVariable<T2> d2);
}
