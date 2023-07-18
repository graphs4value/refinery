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
import tools.refinery.store.query.rewriter.AbstractRecursiveRewriter;
import tools.refinery.store.reasoning.interpretation.PartialRelationRewriter;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.*;

class PartialQueryRewriter extends AbstractRecursiveRewriter {
	private final DnfLifter lifter;
	private final Map<PartialRelation, PartialRelationRewriter> relationRewriterMap = new HashMap<>();

	PartialQueryRewriter(DnfLifter lifter) {
		this.lifter = lifter;
	}

	public void addRelationRewriter(PartialRelation partialRelation, PartialRelationRewriter interpreter) {
		if (relationRewriterMap.put(partialRelation, interpreter) != null) {
			throw new IllegalArgumentException("Duplicate partial relation: " + partialRelation);
		}
	}

	@Override
	protected Dnf doRewrite(Dnf dnf) {
		var builder = Dnf.builderFrom(dnf);
		for (var clause : dnf.getClauses()) {
			builder.clause(rewriteClause(clause));
		}
		return builder.build();
	}

	private List<Literal> rewriteClause(DnfClause clause) {
		var completedLiterals = new ArrayList<Literal>();
		var workList = new ArrayDeque<>(clause.literals());
		while (!workList.isEmpty()) {
			var literal = workList.removeFirst();
			rewrite(literal, completedLiterals, workList);
		}
		return completedLiterals;
	}

	private void rewrite(Literal literal, List<Literal> completedLiterals, Deque<Literal> workList) {
		if (!(literal instanceof AbstractCallLiteral callLiteral)) {
			completedLiterals.add(literal);
			return;
		}
		var target = callLiteral.getTarget();
		if (target instanceof Dnf dnf) {
			rewriteRecursively(callLiteral, dnf, completedLiterals);
		} else if (target instanceof ModalConstraint modalConstraint) {
			var modality = modalConstraint.modality();
			var concreteness = modalConstraint.concreteness();
			var constraint = modalConstraint.constraint();
			if (constraint instanceof Dnf dnf) {
				rewriteRecursively(callLiteral, modality, concreteness, dnf, completedLiterals);
			} else if (constraint instanceof PartialRelation partialRelation) {
				rewrite(callLiteral, modality, concreteness, partialRelation, workList);
			} else {
				throw new IllegalArgumentException("Cannot interpret modal constraint: " + modalConstraint);
			}
		} else {
			completedLiterals.add(literal);
		}
	}

	private void rewriteRecursively(AbstractCallLiteral callLiteral, Modality modality, Concreteness concreteness,
									Dnf dnf, List<Literal> completedLiterals) {
		var liftedDnf = lifter.lift(modality, concreteness, dnf);
		rewriteRecursively(callLiteral, liftedDnf, completedLiterals);
	}

	private void rewriteRecursively(AbstractCallLiteral callLiteral, Dnf dnf, List<Literal> completedLiterals) {
		var rewrittenDnf = rewrite(dnf);
		var rewrittenLiteral = callLiteral.withTarget(rewrittenDnf);
		completedLiterals.add(rewrittenLiteral);
	}

	private void rewrite(AbstractCallLiteral callLiteral, Modality modality, Concreteness concreteness,
						 PartialRelation partialRelation, Deque<Literal> workList) {
		var rewriter = relationRewriterMap.get(partialRelation);
		if (rewriter == null) {
			throw new IllegalArgumentException("Do not know how to interpret partial relation: " + partialRelation);
		}
		var literals = rewriter.rewriteLiteral(callLiteral, modality, concreteness);
		int length = literals.size();
		for (int i = length - 1; i >= 0; i--) {
			workList.addFirst(literals.get(i));
		}
	}
}
