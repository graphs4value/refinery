/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.*;

class PartialClauseRewriter {
	private final PartialQueryRewriter rewriter;
	private final List<Literal> completedLiterals = new ArrayList<>();
	private final Deque<Literal> workList = new ArrayDeque<>();
	private final Set<Variable> positiveVariables = new LinkedHashSet<>();
	private final Set<Variable> unmodifiablePositiveVariables = Collections.unmodifiableSet(positiveVariables);

	public PartialClauseRewriter(PartialQueryRewriter rewriter) {
		this.rewriter = rewriter;
	}

	public List<Literal> rewriteClause(DnfClause clause) {
		workList.addAll(clause.literals());
		while (!workList.isEmpty()) {
			var literal = workList.removeFirst();
			rewrite(literal);
		}
		return completedLiterals;
	}

	private void rewrite(Literal literal) {
		if (!(literal instanceof AbstractCallLiteral callLiteral)) {
			markAsDone(literal);
			return;
		}
		var target = callLiteral.getTarget();
		if (target instanceof Dnf dnf) {
			rewriteRecursively(callLiteral, dnf);
		} else if (target instanceof ModalConstraint modalConstraint) {
			var modality = modalConstraint.modality();
			var concreteness = modalConstraint.concreteness();
			var constraint = modalConstraint.constraint();
			if (constraint instanceof Dnf dnf) {
				rewriteRecursively(callLiteral, modality, concreteness, dnf);
			} else if (constraint instanceof PartialRelation partialRelation) {
				rewrite(callLiteral, modality, concreteness, partialRelation);
			} else {
				throw new IllegalArgumentException("Cannot interpret modal constraint: " + modalConstraint);
			}
		} else {
			markAsDone(literal);
		}
	}

	private void markAsDone(Literal literal) {
		completedLiterals.add(literal);
		positiveVariables.addAll(literal.getOutputVariables());
	}

	private void rewriteRecursively(AbstractCallLiteral callLiteral, Modality modality, Concreteness concreteness,
									Dnf dnf) {
		var liftedDnf = rewriter.getLifter().lift(modality, concreteness, dnf);
		rewriteRecursively(callLiteral, liftedDnf);
	}

	private void rewriteRecursively(AbstractCallLiteral callLiteral, Dnf dnf) {
		var rewrittenDnf = rewriter.rewrite(dnf);
		var rewrittenLiteral = callLiteral.withTarget(rewrittenDnf);
		completedLiterals.add(rewrittenLiteral);
	}

	private void rewrite(AbstractCallLiteral callLiteral, Modality modality, Concreteness concreteness,
						 PartialRelation partialRelation) {
		var relationRewriter = rewriter.getRelationRewriter(partialRelation);
		var literals = relationRewriter.rewriteLiteral(
				unmodifiablePositiveVariables, callLiteral, modality, concreteness);
		int length = literals.size();
		for (int i = length - 1; i >= 0; i--) {
			workList.addFirst(literals.get(i));
		}
	}
}
