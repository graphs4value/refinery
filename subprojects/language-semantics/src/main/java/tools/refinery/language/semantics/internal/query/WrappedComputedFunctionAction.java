/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.*;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.literal.ModalitySpecification;
import tools.refinery.store.reasoning.representation.PartialFunction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class WrappedComputedFunctionAction<A extends AbstractValue<A, C>, C> extends WrappedFunctionAction<A, C> {
	private final List<Literal> literals;
	private final Term<A> valueTerm;
	private final List<NodeVariable> helperParameters;
	private FunctionalQuery<A> helper;

	public WrappedComputedFunctionAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialFunction<A, C> partialFunction,
			List<AssertionArgument> problemArguments, List<Literal> literals, Term<A> valueTerm) {
		super(ruleCompiler, preparedRule, partialFunction, problemArguments);
		this.literals = literals;
		this.valueTerm = valueTerm;
		var inputVariables = new LinkedHashSet<>(getPreparedRule().parameterMap().values());
		inputVariables.retainAll(valueTerm.getInputVariables(inputVariables).stream()
				.filter(NodeVariable.class::isInstance)
				.map(NodeVariable.class::cast)
				.collect(Collectors.toUnmodifiableSet()));
		helperParameters = List.copyOf(inputVariables);
	}

	@Override
	protected void findNodeVariables(LinkedHashSet<NodeVariable> parameterSet) {
		super.findNodeVariables(parameterSet);
		parameterSet.addAll(helperParameters);
	}

	@Override
	public boolean toLiterals(boolean positive, ConcreteModality concreteModality, List<Literal> literals) {
		if (helper == null) {
			return true;
		}
		var abstractDomain = getPartialFunction().abstractDomain();
		var helperArguments = new ArrayList<Variable>(helperParameters.size() + 1);
		helperArguments.addAll(helperParameters);
		var output = Variable.of("output", abstractDomain.abstractType());
		helperArguments.add(output);
		var constraint = ModalConstraint.of(ModalitySpecification.UNSPECIFIED, concreteModality.concreteness(),
				helper.getDnf());
		var valueTerm = constraint.leftJoinBy(output, abstractDomain.error(), helperArguments);
		toLiterals(valueTerm, positive, concreteModality, literals);
		return false;
	}

	@Override
	public void setPrecondition(RelationalQuery precondition) {
		var abstractDomain = getPartialFunction().abstractDomain();
		var helperLiterals = new ArrayList<Literal>(literals.size() + 2);
		var dnf = precondition.getDnf();
		var constraint = ModalConstraint.of(Modality.MUST, dnf);
		var preconditionParameters = dnf.getSymbolicParameters().stream()
				.map(SymbolicParameter::getVariable)
				.toList();
		helperLiterals.add(constraint.call(CallPolarity.POSITIVE, preconditionParameters));
		helperLiterals.addAll(literals);
		var output = Variable.of("output", abstractDomain.abstractType());
		helperLiterals.add(output.assign(valueTerm));
		helper = Query.builder()
				.parameters(helperParameters)
				.output(output)
				.clause(helperLiterals)
				.build();
	}

	@Override
	public void toActionLiterals(Concreteness concreteness, List<ActionLiteral> actionLiterals,
								 Map<tools.refinery.language.model.problem.Variable, NodeVariable> localScope) {
		var arguments = List.of(collectArguments(localScope, actionLiterals));
		var dnf = helper.getDnf();
		var constraint = ModalConstraint.of(concreteness, dnf);
		var symbolicParameters = dnf.getSymbolicParameters();
		var parameterVariables = symbolicParameters.stream()
				.map(SymbolicParameter::getVariable)
				.toList();
		var concreteHelper = helper.withDnf(Dnf.builder(helper.name() + "#" + concreteness)
				.symbolicParameters(symbolicParameters)
				.clause(constraint.call(CallPolarity.POSITIVE, parameterVariables))
				.build());
		actionLiterals.add(PartialActionLiterals.mergeComputed(getPartialFunction(), arguments, concreteHelper,
				helperParameters));
	}
}
