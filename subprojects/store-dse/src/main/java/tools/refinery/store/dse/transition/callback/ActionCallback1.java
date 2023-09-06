/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.callback;

import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.query.term.NodeVariable;

import java.util.List;

@FunctionalInterface
public interface ActionCallback1 {
	List<ActionLiteral> toLiterals(NodeVariable v1);
}
