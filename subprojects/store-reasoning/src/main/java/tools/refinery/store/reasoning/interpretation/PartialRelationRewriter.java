/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;
import java.util.Set;

@FunctionalInterface
public interface PartialRelationRewriter {
	List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal, Modality modality,
								 Concreteness concreteness);
}
