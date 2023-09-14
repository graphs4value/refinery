/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.rewriter;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.*;

class ClauseInputParameterResolver {
	private final InputParameterResolver rewriter;
	private final String dnfName;
	private final int clauseIndex;
	private final Set<Variable> positiveVariables = new LinkedHashSet<>();
	private final List<Literal> inlinedLiterals = new ArrayList<>();
	private final Deque<Literal> workList;
	private int helperIndex = 0;

	public ClauseInputParameterResolver(InputParameterResolver rewriter, List<Literal> context, DnfClause clause,
										String dnfName, int clauseIndex) {
		this.rewriter = rewriter;
		this.dnfName = dnfName;
		this.clauseIndex = clauseIndex;
		workList = new ArrayDeque<>(clause.literals().size() + context.size());
		for (var literal : context) {
			workList.addLast(literal);
		}
		for (var literal : clause.literals()) {
			workList.addLast(literal);
		}
	}

	public List<Literal> rewriteClause() {
		while (!workList.isEmpty()) {
			var literal = workList.removeFirst();
			processLiteral(literal);
		}
		return inlinedLiterals;
	}

	private void processLiteral(Literal literal) {
		if (!(literal instanceof AbstractCallLiteral abstractCallLiteral) ||
				!(abstractCallLiteral.getTarget() instanceof Dnf targetDnf)) {
			markAsDone(literal);
			return;
		}
		boolean hasInputParameter = hasInputParameter(targetDnf);
		if (!hasInputParameter) {
			targetDnf = rewriter.rewrite(targetDnf);
		}
		if (inlinePositiveClause(abstractCallLiteral, targetDnf)) {
			return;
		}
		if (eliminateDoubleNegation(abstractCallLiteral, targetDnf)) {
			return;
		}
		if (hasInputParameter) {
			rewriteWithCurrentContext(abstractCallLiteral, targetDnf);
			return;
		}
		markAsDone(abstractCallLiteral.withTarget(targetDnf));
	}

	private void markAsDone(Literal literal) {
		positiveVariables.addAll(literal.getOutputVariables());
		inlinedLiterals.add(literal);
	}

	private boolean inlinePositiveClause(AbstractCallLiteral abstractCallLiteral, Dnf targetDnf) {
		var targetLiteral = getSingleLiteral(abstractCallLiteral, targetDnf, CallPolarity.POSITIVE);
		if (targetLiteral == null) {
			return false;
		}
		var substitution = asSubstitution(abstractCallLiteral, targetDnf);
		var substitutedLiteral = targetLiteral.substitute(substitution);
		workList.addFirst(substitutedLiteral);
		return true;
	}

	private boolean eliminateDoubleNegation(AbstractCallLiteral abstractCallLiteral, Dnf targetDnf) {
		var targetLiteral = getSingleLiteral(abstractCallLiteral, targetDnf, CallPolarity.NEGATIVE);
		if (!(targetLiteral instanceof CallLiteral targetCallLiteral) ||
				targetCallLiteral.getPolarity() != CallPolarity.NEGATIVE) {
			return false;
		}
		var substitution = asSubstitution(abstractCallLiteral, targetDnf);
		var substitutedLiteral = (CallLiteral) targetCallLiteral.substitute(substitution);
		workList.addFirst(substitutedLiteral.negate());
		return true;
	}

	private void rewriteWithCurrentContext(AbstractCallLiteral abstractCallLiteral, Dnf targetDnf) {
		var contextBuilder = Dnf.builder("%s#clause%d#helper%d".formatted(dnfName, clauseIndex, helperIndex));
		helperIndex++;
		contextBuilder.parameters(positiveVariables, ParameterDirection.OUT);
		contextBuilder.clause(inlinedLiterals);
		var contextDnf = contextBuilder.build();
		var contextCall = new CallLiteral(CallPolarity.POSITIVE, contextDnf, List.copyOf(positiveVariables));
		inlinedLiterals.clear();
		var substitution = Substitution.builder().renewing().build();
		var context = new ArrayList<Literal>();
		context.add(contextCall.substitute(substitution));
		int arity = targetDnf.arity();
		for (int i = 0; i < arity; i++) {
			var parameter = targetDnf.getSymbolicParameters().get(i).getVariable();
			var argument = abstractCallLiteral.getArguments().get(i);
			context.add(new EquivalenceLiteral(true, parameter, substitution.getSubstitute(argument)));
		}
		var rewrittenDnf = rewriter.rewriteWithContext(context, targetDnf);
		workList.addFirst(abstractCallLiteral.withTarget(rewrittenDnf));
		workList.addFirst(contextCall);
	}

	private static boolean hasInputParameter(Dnf targetDnf) {
		for (var parameter : targetDnf.getParameters()) {
			if (parameter.getDirection() != ParameterDirection.OUT) {
				return true;
			}
		}
		return false;
	}

	private static Literal getSingleLiteral(AbstractCallLiteral abstractCallLiteral, Dnf targetDnf,
											CallPolarity polarity) {
		if (!(abstractCallLiteral instanceof CallLiteral callLiteral) ||
				callLiteral.getPolarity() != polarity) {
			return null;
		}
		var clauses = targetDnf.getClauses();
		if (clauses.size() != 1) {
			return null;
		}
		var targetLiterals = clauses.get(0).literals();
		if (targetLiterals.size() != 1) {
			return null;
		}
		return targetLiterals.get(0);
	}

	private static Substitution asSubstitution(AbstractCallLiteral callLiteral, Dnf targetDnf) {
		var builder = Substitution.builder().renewing();
		var arguments = callLiteral.getArguments();
		var parameters = targetDnf.getSymbolicParameters();
		int arity = arguments.size();
		if (parameters.size() != arity) {
			throw new IllegalArgumentException("Call %s of %s arity mismatch".formatted(callLiteral, targetDnf));
		}
		for (int i = 0; i < arity; i++) {
			builder.putChecked(parameters.get(i).getVariable(), arguments.get(i));
		}
		return builder.build();
	}
}
