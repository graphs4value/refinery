/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import org.jetbrains.annotations.NotNull;
import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

class WrappedComputedRelationAction extends TermBasedWrappedAction<TruthValue, Boolean> {
	private final ComputedHelper<TruthValue, Boolean> helper;

	public WrappedComputedRelationAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialRelation partialRelation,
			List<AssertionArgument> problemArguments, List<Literal> literals, Term<TruthValue> valueTerm) {
		super(ruleCompiler, preparedRule, partialRelation, problemArguments);
		helper = new ComputedHelper<>(preparedRule, partialRelation, literals, valueTerm);
	}

	public PartialRelation getPartialRelation() {
		return (PartialRelation) super.getPartialSymbol();
	}

	@Override
	protected void findNodeVariables(LinkedHashSet<NodeVariable> parameterSet) {
		super.findNodeVariables(parameterSet);
		helper.findNodeVariables(parameterSet);
	}

	@Override
	protected @NotNull Term<TruthValue> getCurrentTerm(ConcretenessSpecification concreteness,
													   List<NodeVariable> arguments) {
		var constraint = ModalConstraint.of(ModalitySpecification.UNSPECIFIED, concreteness, getPartialRelation());
		return new ReifyTerm(constraint, Collections.unmodifiableList(arguments));
	}

	@Override
	protected Term<TruthValue> getValueTerm(ConcretenessSpecification concreteness) {
		return helper.toValueTerm(concreteness);
	}

	@Override
	public void setPrecondition(RelationalQuery precondition) {
		helper.setPrecondition(precondition);
	}

	@Override
	protected ActionLiteral getActionLiteral(Concreteness concreteness, List<NodeVariable> arguments) {
		return helper.toActionLiteral(concreteness, arguments);
	}
}
