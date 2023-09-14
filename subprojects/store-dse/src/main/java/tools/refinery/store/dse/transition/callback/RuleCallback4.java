/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.callback;

import tools.refinery.store.dse.transition.RuleBuilder;
import tools.refinery.store.query.term.NodeVariable;

@FunctionalInterface
public interface RuleCallback4 {
	void accept(RuleBuilder builder, NodeVariable p1, NodeVariable p2, NodeVariable p3, NodeVariable p4);
}
