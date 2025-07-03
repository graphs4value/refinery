/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.List;

public class QueryBasedFunctionRewriter<A extends AbstractValue<A, C>, C> implements PartialFunctionRewriter<A, C> {
	private final FunctionalQuery<A> partialQuery;
	private final FunctionalQuery<A> candidateQuery;
	private final A errorValue;

	public QueryBasedFunctionRewriter(FunctionalQuery<A> partialQuery, FunctionalQuery<A> candidateQuery,
									  AbstractDomain<A, C> abstractDomain) {
		this.partialQuery = partialQuery;
		this.candidateQuery = candidateQuery;
		this.errorValue = abstractDomain.error();
	}

	@Override
	public Term<A> rewritePartialFunctionCall(Concreteness concreteness, List<NodeVariable> arguments) {
		return getQuery(concreteness).leftJoin(errorValue, arguments);
	}

	private FunctionalQuery<A> getQuery(Concreteness concreteness) {
		return switch (concreteness) {
			case PARTIAL -> partialQuery;
			case CANDIDATE -> candidateQuery;
		};
	}
}
