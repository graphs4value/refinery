/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf.callback;

import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;

import java.util.Collection;

@FunctionalInterface
public interface ClauseCallback2Data2<T1, T2> {
	Collection<Literal> toLiterals(DataVariable<T1> x1, DataVariable<T2> x2);
}
