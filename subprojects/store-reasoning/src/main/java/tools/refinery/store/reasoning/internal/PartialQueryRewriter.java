/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.rewriter.AbstractRecursiveRewriter;
import tools.refinery.store.reasoning.interpretation.PartialRelationRewriter;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.HashMap;
import java.util.Map;

class PartialQueryRewriter extends AbstractRecursiveRewriter {
	private final DnfLifter lifter;
	private final Map<PartialRelation, PartialRelationRewriter> relationRewriterMap = new HashMap<>();

	PartialQueryRewriter(DnfLifter lifter) {
		this.lifter = lifter;
	}

	DnfLifter getLifter() {
		return lifter;
	}

	PartialRelationRewriter getRelationRewriter(PartialRelation partialRelation) {
		var rewriter = relationRewriterMap.get(partialRelation);
		if (rewriter == null) {
			throw new IllegalArgumentException("Do not know how to interpret partial relation: " + partialRelation);
		}
		return rewriter;
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
			var clauseRewriter = new PartialClauseRewriter(this);
			var rewrittenClauses = clauseRewriter.rewriteClause(clause);
			builder.clause(rewrittenClauses);
		}
		return builder.build();
	}
}
