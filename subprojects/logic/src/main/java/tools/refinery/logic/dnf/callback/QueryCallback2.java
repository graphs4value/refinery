/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.dnf.callback;

import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.term.NodeVariable;

@FunctionalInterface
public interface QueryCallback2 {
	void accept(QueryBuilder builder, NodeVariable p1, NodeVariable p2);
}
