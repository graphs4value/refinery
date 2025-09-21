/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import com.google.inject.Inject;
import tools.refinery.language.expressions.BuiltInTerms;
import tools.refinery.language.model.problem.AssignmentExpr;
import tools.refinery.language.model.problem.FunctionDefinition;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.typesystem.DataExprType;
import tools.refinery.language.typesystem.ProblemTypeAnalyzer;
import tools.refinery.language.typesystem.SignatureProvider;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.*;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.literal.PartialAggregationTerm;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.*;

public class FunctionCompiler {
	@Inject
	private ProblemTypeAnalyzer problemTypeAnalyzer;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	@Inject
	private SignatureProvider signatureProvider;

	private QueryCompiler queryCompiler;

	public void setQueryCompiler(QueryCompiler queryCompiler) {
		this.queryCompiler = queryCompiler;
	}

	public RelationalQuery toDomainQuery(String name, FunctionDefinition functionDefinition) {
		var preparedQuery = queryCompiler.prepareParameters(functionDefinition);
		var builder = Query.builder(name).parameters(preparedQuery.parameters());
		if (ProblemUtil.isBaseFunction(functionDefinition) || ProblemUtil.isSingleExpression(functionDefinition)) {
			return builder.clause(preparedQuery.commonLiterals()).build();
		}
		for (var match : functionDefinition.getCases()) {
			queryCompiler.buildConjunction(match.getCondition(), preparedQuery.parameterMap(),
					preparedQuery.commonLiterals(), builder);
		}
		return builder.build();
	}

	public <A extends AbstractValue<A, C>, C> FunctionalQuery<A> toQuery(
			String name, AbstractDomain<A, C> abstractDomain, FunctionDefinition functionDefinition,
			PartialRelation domainRelation) {
		if (ProblemUtil.isBaseFunction(functionDefinition)) {
			throw new IllegalArgumentException("Trying to build query for base function: " + name);
		}
		var preparedQuery = queryCompiler.prepareParameters(functionDefinition);
		var literals = new ArrayList<Literal>();
		literals.add(ModalConstraint.of(Modality.MAY, domainRelation).call(preparedQuery.parameters()));
		AnyTerm term;
		if (ProblemUtil.isSingleExpression(functionDefinition)) {
			term = compileSimpleQuery(functionDefinition, preparedQuery, literals);
		} else if (isWithoutQuantification(functionDefinition)) {
			term = compileWithoutQuantification(functionDefinition, preparedQuery, literals);
		} else {
			term = compileComplexQuery(name, abstractDomain, functionDefinition, domainRelation, preparedQuery);
		}
		var output = Variable.of("output", abstractDomain.abstractType());
		literals.add(output.assign(term.asType(abstractDomain.abstractType())));
		return Query.builder(name)
				.parameters(preparedQuery.parameters())
				.output(output)
				.clause(literals)
				.build();
	}

	private AnyTerm compileSimpleQuery(FunctionDefinition functionDefinition,
									   QueryCompiler.PreparedQuery preparedQuery, ArrayList<Literal> literals) {
		var expr = functionDefinition.getCases().getFirst().getCondition().getLiterals().getFirst();
		return queryCompiler.interpretTerm(expr, preparedQuery.parameterMap(), literals);
	}

	private boolean isWithoutQuantification(FunctionDefinition functionDefinition) {
		var cases = functionDefinition.getCases();
		if (cases.size() != 1) {
			return false;
		}
		var condition = cases.getFirst().getCondition();
		return condition != null && condition.getImplicitVariables().stream()
				.allMatch(variable -> problemTypeAnalyzer.getVariableType(variable) instanceof DataExprType);
	}

	private AnyTerm compileWithoutQuantification(
			FunctionDefinition functionDefinition, QueryCompiler.PreparedQuery preparedQuery,
			ArrayList<Literal> literals) {
		AnyTerm term;
		var match = functionDefinition.getCases().getFirst();
		var condition = match.getCondition();
		var localScope = queryCompiler.extendScope(preparedQuery.parameterMap(), condition.getImplicitVariables());
		for (var expr : condition.getLiterals()) {
			// The only way to add a variable with type {@link DataExprType} to the scope is through an assignment
			// expression, so we can ignore all other expressions. Parameter variables have already been bound by
			// the call to {@code may domainRelation} above.
			if (expr instanceof AssignmentExpr) {
				queryCompiler.toLiteralsTraced(expr, localScope, literals);
			}
		}
		term = queryCompiler.interpretTerm(match.getValue(), localScope, literals);
		return term;
	}

	private <A extends AbstractValue<A, C>, C> AnyTerm compileComplexQuery(
			String name, AbstractDomain<A, C> abstractDomain, FunctionDefinition functionDefinition,
			PartialRelation domainRelation, QueryCompiler.PreparedQuery preparedQuery) {
		AnyTerm term;
		var termInterpreter = importAdapterProvider.getTermInterpreter(functionDefinition);
		if (!(signatureProvider.getSignature(functionDefinition).resultType() instanceof DataExprType dataType)) {
			throw new IllegalArgumentException("Unsupported data type for function: " + name);
		}
		// We know the type of the join and meet aggregators in advance.
		@SuppressWarnings("unchecked")
		var joinAggregator = (PartialAggregator<A, C, A, C>) termInterpreter
				.getAggregator(BuiltInTerms.JOIN_AGGREGATOR, dataType)
				.orElseThrow(() -> new IllegalArgumentException("Invalid join aggregator"));
		@SuppressWarnings("unchecked")
		var meetAggregator = (PartialAggregator<A, C, A, C>) termInterpreter
				.getAggregator(BuiltInTerms.MEET_AGGREGATOR, dataType)
				.orElseThrow(() -> new IllegalArgumentException("Invalid meet aggregator"));
		var cases = functionDefinition.getCases();
		var parameterSet = Set.of(preparedQuery.parameters());
		List<Term<A>> mayTerms = new ArrayList<>(cases.size());
		List<Term<A>> mustTerms = new ArrayList<>(cases.size());
		int i = 0;
		for (var match : cases) {
			var condition = match.getCondition();
			var localScope = queryCompiler.extendScope(preparedQuery.parameterMap(),
					condition.getImplicitVariables());
			var helperLiterals = new ArrayList<Literal>();
			helperLiterals.add(domainRelation.call(preparedQuery.parameters()));
			for (var expr : condition.getLiterals()) {
				queryCompiler.toLiteralsTraced(expr, localScope, helperLiterals);
			}
			var helperParameters = new ArrayList<Variable>(localScope.values());
			var helper = Dnf.builder(name + "#case" + i)
					.parameters(helperParameters)
					.clause(helperLiterals)
					.build();
			var bodyTerm = queryCompiler.interpretTerm(match.getValue(), localScope, helperLiterals)
					.asType(abstractDomain.abstractType());
			mayTerms.add(new PartialAggregationTerm<>(joinAggregator, helper, List.copyOf(helperParameters),
					bodyTerm));
			// Renew private variables.
			var renewedHelperParameters = new ArrayList<Variable>(helperParameters.size());
			var substitutionBuilder = Substitution.builder().partial();
			for (var variable : helperParameters) {
				if (variable instanceof NodeVariable && parameterSet.contains(variable)) {
					renewedHelperParameters.add(variable);
				} else {
					var newVariable = variable.renew();
					renewedHelperParameters.add(newVariable);
					substitutionBuilder.putChecked(variable, newVariable);
				}
			}
			mustTerms.add(new PartialAggregationTerm<>(meetAggregator, helper, List.copyOf(renewedHelperParameters),
					bodyTerm.substitute(substitutionBuilder.build())));
			i++;
		}
		var mayResult = mayTerms.stream()
				.reduce((left, right) -> AbstractDomainTerms.join(abstractDomain, left, right))
				.orElseThrow(() -> new IllegalArgumentException("No may terms"));
		var mustResult = mustTerms.stream()
				.reduce((left, right) -> AbstractDomainTerms.meet(abstractDomain, left, right))
				.orElseThrow(() -> new IllegalArgumentException("No must terms"));
		return AbstractDomainTerms.meet(abstractDomain, mayResult, mustResult);
	}
}
