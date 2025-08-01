/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.language.model.problem.NodeAssertionArgument;
import tools.refinery.language.model.problem.Variable;
import tools.refinery.language.model.problem.VariableOrNode;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

abstract class WrappedAction {
	private final RuleCompiler ruleCompiler;
	private final PreparedRule preparedRule;
	private final List<AssertionArgument> problemArguments;

	protected WrappedAction(RuleCompiler ruleCompiler, PreparedRule preparedRule,
							List<AssertionArgument> problemArguments) {
		this.ruleCompiler = ruleCompiler;
		this.preparedRule = preparedRule;
		this.problemArguments = problemArguments;
	}

	protected RuleCompiler getRuleCompiler() {
		return ruleCompiler;
	}

	protected PreparedRule getPreparedRule() {
		return preparedRule;
	}

	public List<AssertionArgument> getProblemArguments() {
		return problemArguments;
	}

	public List<NodeVariable> getNodeVariables() {
		var parameterSet = new LinkedHashSet<NodeVariable>();
		findNodeVariables(parameterSet);
		return List.copyOf(parameterSet);
	}

	protected void findNodeVariables(LinkedHashSet<NodeVariable> parameterSet) {
		for (AssertionArgument argument : problemArguments) {
			if (argument instanceof NodeAssertionArgument nodeAssertionArgument) {
				VariableOrNode variableOrNode = nodeAssertionArgument.getNode();
				if (variableOrNode instanceof Variable problemVariable) {
					NodeVariable nodeVariable = preparedRule.parameterMap().get(problemVariable);
					parameterSet.add(nodeVariable);
				}
			}
		}
	}

	protected NodeVariable[] collectArguments(ConcreteModality concreteModality, List<Literal> literals) {
		return ruleCompiler.collectArguments(problemArguments, preparedRule, concreteModality, literals);
	}

	protected NodeVariable[] collectArguments(
			Map<tools.refinery.language.model.problem.Variable, NodeVariable> localScope,
			List<ActionLiteral> actionLiterals) {
		return ruleCompiler.collectArguments(problemArguments, localScope, actionLiterals);
	}

	/**
	 * Adds the precondition of the action to the given list of literals, or delays them if until
	 * {@link #setPrecondition(RelationalQuery)} is called.
	 *
	 * @param positive         {@code true} to add the precondition positively, {@code false} to add it negatively.
	 * @param concreteModality The modality of the precondition.
	 * @param literals         The list of literals to add the precondition to.
	 * @return {@code true} if the precondition should be delayed, {@code false} if it was added immediately.
	 */
	public abstract boolean toLiterals(boolean positive, ConcreteModality concreteModality, List<Literal> literals);

	public void setPrecondition(RelationalQuery precondition) {
		// No need to access precondition by default.
	}

	public void toLiteralsChecked(boolean positive, ConcreteModality concreteModality, List<Literal> literals) {
		if (toLiterals(positive, concreteModality, literals)) {
			throw new IllegalStateException("Can't transform into literals");
		}
	}

	public abstract void toActionLiterals(Concreteness concreteness, List<ActionLiteral> actionLiterals,
										  Map<Variable, NodeVariable> localScope);
}
