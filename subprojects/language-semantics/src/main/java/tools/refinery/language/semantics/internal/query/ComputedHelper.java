/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.*;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

class ComputedHelper<A extends AbstractValue<A, C>, C> {
	private final PartialSymbol<A, C> partialSymbol;
	private final List<Literal> literals;
	private final Term<A> valueTerm;
	private final List<NodeVariable> helperParameters;
	private FunctionalQuery<A> helper;

	ComputedHelper(PreparedRule preparedRule, PartialSymbol<A, C> partialSymbol, List<Literal> literals,
				   Term<A> valueTerm) {
		this.partialSymbol = partialSymbol;
		this.literals = literals;
		this.valueTerm = valueTerm;
		var inputVariables = new LinkedHashSet<>(preparedRule.parameterMap().values());
		inputVariables.retainAll(valueTerm.getInputVariables(inputVariables).stream()
				.filter(NodeVariable.class::isInstance)
				.map(NodeVariable.class::cast)
				.collect(Collectors.toUnmodifiableSet()));
		helperParameters = List.copyOf(inputVariables);
	}

	public void findNodeVariables(LinkedHashSet<NodeVariable> parameterSet) {
		parameterSet.addAll(helperParameters);
	}

	public @Nullable Term<A> toValueTerm(ConcretenessSpecification concreteness) {
		if (helper == null) {
			return null;
		}
		var abstractDomain = partialSymbol.abstractDomain();
		var helperArguments = new ArrayList<Variable>(helperParameters.size() + 1);
		helperArguments.addAll(helperParameters);
		var output = Variable.of("output", abstractDomain.abstractType());
		helperArguments.add(output);
		var constraint = ModalConstraint.of(ModalitySpecification.UNSPECIFIED, concreteness, helper.getDnf());
		return constraint.leftJoinBy(output, abstractDomain.error(), helperArguments);
	}

	public void setPrecondition(RelationalQuery precondition) {
		var abstractDomain = partialSymbol.abstractDomain();
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

	public ActionLiteral toActionLiteral(Concreteness concreteness, List<NodeVariable> arguments) {
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
		return PartialActionLiterals.mergeComputed(partialSymbol, arguments, concreteHelper,
				helperParameters);
	}
}
