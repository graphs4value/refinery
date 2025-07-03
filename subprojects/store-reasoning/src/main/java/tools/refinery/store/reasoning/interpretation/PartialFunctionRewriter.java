/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.List;

@FunctionalInterface
public interface PartialFunctionRewriter<A extends AbstractValue<A, C>, C> {
	Term<A> rewritePartialFunctionCall(Concreteness concreteness, List<NodeVariable> arguments);
}
