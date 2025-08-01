/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.literal.BooleanLiteral;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.List;
import java.util.Map;

class WrappedRelationAction extends WrappedAction {
	private final PartialRelation partialRelation;
	private final TruthValue truthValue;

	public WrappedRelationAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialRelation partialRelation,
			List<AssertionArgument> problemArguments, TruthValue truthValue) {
		super(ruleCompiler, preparedRule, problemArguments);
		this.partialRelation = partialRelation;
		this.truthValue = truthValue;
	}

	@Override
	public boolean toLiterals(boolean positive, ConcreteModality concreteModality, List<Literal> literals) {
		if (truthValue == TruthValue.UNKNOWN) {
			if (!positive) {
				literals.add(BooleanLiteral.FALSE);
			}
			return false;
		}
		var arguments = collectArguments(concreteModality, literals);
		if (truthValue == TruthValue.ERROR ||
				(truthValue == TruthValue.TRUE && positive) ||
				(truthValue == TruthValue.FALSE && !positive)) {
			literals.add(concreteModality.wrapConstraint(partialRelation).call(CallPolarity.POSITIVE, arguments));
		}
		if (truthValue == TruthValue.ERROR ||
				(truthValue == TruthValue.FALSE && positive) ||
				(truthValue == TruthValue.TRUE && !positive)) {
			literals.add(concreteModality.negate().wrapConstraint(partialRelation)
					.call(CallPolarity.NEGATIVE, arguments));
		}
		return false;
	}

	@Override
	public void toActionLiterals(Concreteness concreteness, List<ActionLiteral> actionLiterals,
								 Map<tools.refinery.language.model.problem.Variable, NodeVariable> localScope) {
		var arguments = collectArguments(localScope, actionLiterals);
		actionLiterals.add(PartialActionLiterals.merge(partialRelation, truthValue, arguments));
	}
}
