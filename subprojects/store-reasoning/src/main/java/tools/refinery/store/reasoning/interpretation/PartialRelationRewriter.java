/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.AbstractCallTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;
import java.util.Set;

public interface PartialRelationRewriter {
	List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal, Modality modality,
								 Concreteness concreteness);

	<T> Term<T> rewriteTerm(AbstractCallTerm<T> term, Modality modality, Concreteness concreteness);
}
