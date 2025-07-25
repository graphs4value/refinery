/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.FunctionDefinition;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.ArrayList;

public class FunctionCompiler {
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
		Term<A> term;
		if (ProblemUtil.isSingleExpression(functionDefinition)) {
			var expr = functionDefinition.getCases().getFirst().getCondition().getLiterals().getFirst();
			term = queryCompiler.interpretTerm(expr, preparedQuery.parameterMap(), literals)
					.asType(abstractDomain.abstractType());
		} else {
			throw new IllegalArgumentException("Complex functions are not currently supported.");
		}
		var output = Variable.of("output", abstractDomain.abstractType());
		literals.add(output.assign(term));
		return Query.builder(name)
				.parameters(preparedQuery.parameters())
				.output(output)
				.clause(literals)
				.build();
	}
}
