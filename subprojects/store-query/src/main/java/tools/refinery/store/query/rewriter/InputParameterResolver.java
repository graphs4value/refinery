/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.rewriter;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfBuilder;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.HashSet;
import java.util.List;

public class InputParameterResolver extends AbstractRecursiveRewriter {
	@Override
	protected Dnf doRewrite(Dnf dnf) {
		return rewriteWithContext(List.of(), dnf);
	}

	Dnf rewriteWithContext(List<Literal> context, Dnf dnf) {
		var dnfName = dnf.name();
		var builder = Dnf.builder(dnfName);
		createSymbolicParameters(context, dnf, builder);
		builder.functionalDependencies(dnf.getFunctionalDependencies());
		var clauses = dnf.getClauses();
		int clauseCount = clauses.size();
		for (int i = 0; i < clauseCount; i++) {
			var clause = clauses.get(i);
			var clauseRewriter = new ClauseInputParameterResolver(this, context, clause, dnfName, i);
			builder.clause(clauseRewriter.rewriteClause());
		}
		return builder.build();
	}

	private static void createSymbolicParameters(List<Literal> context, Dnf dnf, DnfBuilder builder) {
		var positiveInContext = new HashSet<Variable>();
		for (var literal : context) {
			positiveInContext.addAll(literal.getOutputVariables());
		}
		for (var symbolicParameter : dnf.getSymbolicParameters()) {
			var variable = symbolicParameter.getVariable();
			var isOutput = symbolicParameter.getDirection() == ParameterDirection.OUT ||
					positiveInContext.contains(variable);
			var direction = isOutput ? ParameterDirection.OUT : ParameterDirection.IN;
			builder.parameter(variable, direction);
		}
	}
}
