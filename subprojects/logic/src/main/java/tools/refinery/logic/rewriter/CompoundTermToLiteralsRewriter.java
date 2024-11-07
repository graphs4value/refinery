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
			if (outputLiterals.isEmpty()) {
				rewrittenLiterals.add(literal);
			} else {
				int size = outputLiterals.size();
				for (int i = size - 1; i >= 0; i--) {
					workList.addFirst(outputLiterals.get(i));
				}
				outputLiterals.clear();
			}
		}

		private <T> void processAssignLiteral(AssignLiteral<T> assignLiteral) {
			var targetVariable = assignLiteral.getVariable();
			var term = assignLiteral.getTerm();
			var rewrittenTerm = rewriteTerm(term, () -> targetVariable);
			if (term != rewrittenTerm) {
				if (!Objects.equals(targetVariable, rewrittenTerm)) {
					workList.addFirst(new AssignLiteral<>(targetVariable, rewrittenTerm));
				}
				return;
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
			return switch (term) {
				case LeftJoinTerm<T> leftJoinTerm -> rewriteLeftJoinTerm(leftJoinTerm, variableSupplier);
				case CountTerm countTerm -> rewriteCountTerm(countTerm, variableSupplier);
				case AggregationTerm<T, ?> aggregationTerm -> rewriteAggregationTerm(aggregationTerm,
						variableSupplier);
				default -> term.rewriteSubTerms(this::rewriteTerm);
			};
		}

		private <T> DataVariable<T> rewriteLeftJoinTerm(LeftJoinTerm<T> leftJoinTerm,
														Supplier<DataVariable<T>> variableSupplier) {
			var temporaryVariable = variableSupplier.get();
			outputLiterals.add(new LeftJoinLiteral<>(temporaryVariable, leftJoinTerm.getPlaceholderVariable(),
					leftJoinTerm.getDefaultValue(), leftJoinTerm.getTarget(), leftJoinTerm.getArguments()));
			return temporaryVariable;
		}

		private <T> DataVariable<T> rewriteCountTerm(CountTerm countTerm, Supplier<DataVariable<T>> variableSupplier) {
			var temporaryVariable = variableSupplier.get();
			// The only choice for {@code T} in {@link CountTerm} is {@link Integer}.
			@SuppressWarnings("unchecked")
			var temporaryIntegerVariable = (DataVariable<Integer>) temporaryVariable;
			outputLiterals.add(new CountLiteral(temporaryIntegerVariable, countTerm.getTarget(),
					countTerm.getArguments()));
			return temporaryVariable;
		}

		private <R, T> DataVariable<R> rewriteAggregationTerm(AggregationTerm<R, T> aggregationTerm,
															  Supplier<DataVariable<R>> variableSupplier) {
			var temporaryVariable = variableSupplier.get();
			outputLiterals.add(new AggregationLiteral<>(temporaryVariable, aggregationTerm.getAggregator(),
					aggregationTerm.getInputVariable(), aggregationTerm.getTarget(), aggregationTerm.getArguments()));
			return temporaryVariable;
		}
	}
}
