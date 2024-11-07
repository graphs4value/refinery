/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.AbstractCallTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;
import java.util.Set;

public abstract class TargetRewriter implements PartialRelationRewriter {
	@Override
	public List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal,
										Modality modality, Concreteness concreteness) {
		var newTarget = getTarget(modality, concreteness);
		return List.of(literal.withTarget(newTarget));
	}

	protected abstract Constraint getTarget(Modality modality, Concreteness concreteness);

	@Override
	public <T> Term<T> rewriteTerm(AbstractCallTerm<T> term, Modality modality, Concreteness concreteness) {
		var newTarget = getTarget(modality, concreteness);
		return term.withTarget(newTarget);
	}
}
