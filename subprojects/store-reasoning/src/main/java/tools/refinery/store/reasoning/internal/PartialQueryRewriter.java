/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.rewriter.AbstractRecursiveRewriter;
import tools.refinery.store.reasoning.interpretation.PartialFunctionRewriter;
import tools.refinery.store.reasoning.interpretation.PartialRelationRewriter;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.HashMap;
import java.util.Map;

class PartialQueryRewriter extends AbstractRecursiveRewriter {
	private final DnfLifter lifter;
	private final Map<PartialRelation, PartialRelationRewriter> relationRewriterMap = new HashMap<>();
	private final Map<AnyPartialFunction, PartialFunctionRewriter<?, ?>> functionRewriterMap = new HashMap<>();

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

	<A extends AbstractValue<A, C>, C> PartialFunctionRewriter<A, C> getFunctionRewriter(
			PartialFunction<A, C> partialFunction) {
		// The method {@code addFunctionRewriter} ensures that all partial function rewriter instances are well-typed.
		@SuppressWarnings("unchecked")
		var rewriter = (PartialFunctionRewriter<A, C>) functionRewriterMap.get(partialFunction);
		if (rewriter == null) {
			throw new IllegalArgumentException("Do not know how to interpret partial function: " + partialFunction);
		}
		return rewriter;
	}

	public void addRelationRewriter(PartialRelation partialRelation, PartialRelationRewriter interpreter) {
		if (relationRewriterMap.put(partialRelation, interpreter) != null) {
			throw new IllegalArgumentException("Duplicate partial relation: " + partialRelation);
		}
	}

	public <A extends AbstractValue<A, C>, C> void addFunctionRewriter(PartialFunction<A, C> partialFunction,
																	   PartialFunctionRewriter<A, C> interpreter) {
		if (functionRewriterMap.put(partialFunction, interpreter) != null) {
			throw new IllegalArgumentException("Duplicate partial function: " + partialFunction);
		}
	}

	@Override
	protected Dnf doRewrite(Dnf dnf) {
		var builder = Dnf.builderFrom(dnf);
		for (var clause : dnf.getClauses()) {
			var clauseRewriter = new PartialClauseRewriter(this);
			var rewrittenLiterals = clauseRewriter.rewriteClause(clause);
			builder.clause(rewrittenLiterals);
		}
		return builder.build();
	}
}
