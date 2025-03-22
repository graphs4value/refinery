/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.rewriter;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.DnfClause;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.term.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CompoundTermToLiteralsRewriter extends AbstractRecursiveRewriter {
	@Override
	protected Dnf doRewrite(Dnf dnf) {
		var builder = Dnf.builderFrom(dnf);
		for (var clause : dnf.getClauses()) {
			var clauseRewriter = new ClauseRewriter(clause);
			var rewrittenLiterals = clauseRewriter.rewriteClause();
			builder.clause(rewrittenLiterals);
		}
		return builder.build();
	}

	private class ClauseRewriter {
		private int temporaryCount = 0;
		private final Deque<Literal> workList = new ArrayDeque<>();
		private boolean keepOriginal = true;
		private final List<Literal> outputLiterals = new ArrayList<>();
		private final List<Literal> rewrittenLiterals = new ArrayList<>();

		public ClauseRewriter(DnfClause clause) {
			workList.addAll(clause.literals());
		}

		public List<Literal> rewriteClause() {
			while (!workList.isEmpty()) {
				var literal = workList.removeFirst();
				rewrite(literal);
			}
			return rewrittenLiterals;
		}

		private void rewrite(Literal literal) {
			if (literal instanceof AbstractCallLiteral callLiteral) {
				var target = callLiteral.getTarget();
				if (target instanceof Dnf dnf) {
					var newTarget = CompoundTermToLiteralsRewriter.this.rewrite(dnf);
					rewrittenLiterals.add(callLiteral.withTarget(newTarget));
					return;
				}
			} else if (literal instanceof AssignLiteral<?> assignLiteral) {
				processAssignLiteral(assignLiteral);
			} else if (literal instanceof CheckLiteral checkLiteral) {
				processCheckLiteral(checkLiteral);
			}
			if (outputLiterals.isEmpty() && keepOriginal) {
				rewrittenLiterals.add(literal);
			} else {
				int size = outputLiterals.size();
				for (int i = size - 1; i >= 0; i--) {
					workList.addFirst(outputLiterals.get(i));
				}
				outputLiterals.clear();
				keepOriginal = true;
			}
		}

		private <T> void processAssignLiteral(AssignLiteral<T> assignLiteral) {
			var targetVariable = assignLiteral.getVariable();
			var term = assignLiteral.getTerm();
			if (term instanceof AbstractCallTerm<T> callTerm) {
				// Rewrite the target of the call term if it appears at the top level.
				var target = callTerm.getTarget();
				if (target instanceof Dnf dnf) {
					var newTarget = CompoundTermToLiteralsRewriter.this.rewrite(dnf);
					if (newTarget != target) {
						// We don't need to process this literal again, so we just add it to the result list.
						rewrittenLiterals.add(assignLiteral.withTerm(callTerm.withTarget(newTarget)));
						keepOriginal = false;
						return;
					}
				}
			} else {
				// Extract call terms in the compound term into dedicated assignment literals.
				var rewrittenTerm = rewriteTerm(term, () -> targetVariable);
				if (term != rewrittenTerm) {
					if (!Objects.equals(targetVariable, rewrittenTerm)) {
						workList.addFirst(new AssignLiteral<>(targetVariable, rewrittenTerm));
					}
					return;
				}
			}
			checkNoOutputLiterals(assignLiteral);
		}

		private void processCheckLiteral(CheckLiteral checkLiteral) {
			var term = checkLiteral.getTerm();
			var rewrittenTerm = rewriteTerm(term);
			if (term != rewrittenTerm) {
				workList.addFirst(new CheckLiteral(rewrittenTerm));
				return;
			}
			checkNoOutputLiterals(checkLiteral);
		}

		private void checkNoOutputLiterals(Literal currentLiteral) {
			if (!outputLiterals.isEmpty()) {
				throw new IllegalStateException(
						"Rewriting the term %s produced output literals [%s], but did not change the term."
								.formatted(currentLiteral, outputLiterals.stream()
										.map(Object::toString)
										.collect(Collectors.joining(", "))));
			}
		}

		private <T> Term<T> rewriteTerm(Term<T> term) {
			return rewriteTerm(term, () -> {
				var variableName = "#temp#" + temporaryCount;
				temporaryCount++;
				return Variable.of(variableName, term.getType());
			});
		}

		private <T> Term<T> rewriteTerm(Term<T> term, Supplier<DataVariable<T>> variableSupplier) {
			if (term instanceof AbstractCallTerm<T> callTerm) {
				var temporaryVariable = variableSupplier.get();
				outputLiterals.add(new AssignLiteral<>(temporaryVariable, callTerm));
				return temporaryVariable;
			}
			return term.rewriteSubTerms(this::rewriteTerm);
		}
	}
}
