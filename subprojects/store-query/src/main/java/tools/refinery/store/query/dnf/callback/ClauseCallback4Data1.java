/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf.callback;

import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.NodeVariable;

import java.util.Collection;

@FunctionalInterface
public interface ClauseCallback4Data1<T> {
	Collection<Literal> toLiterals(NodeVariable v1, NodeVariable v2, NodeVariable v3, DataVariable<T> d1);
}
