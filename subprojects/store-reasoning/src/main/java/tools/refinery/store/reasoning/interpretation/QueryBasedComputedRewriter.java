/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;
import java.util.Set;

public class QueryBasedComputedRewriter extends QueryBasedRelationRewriter {
	private final RelationalQuery query;

	public QueryBasedComputedRewriter(RelationalQuery may, RelationalQuery must, RelationalQuery candidateMay,
									  RelationalQuery candidateMust, RelationalQuery computation) {
		super(may, must, candidateMay, candidateMust);
		this.query = computation;
	}

	@Override
	public List<Literal> rewriteComputed(Set<Variable> positiveVariables, AbstractCallLiteral literal,
										 Modality modality, Concreteness concreteness) {
		return List.of(literal.withTarget(ModalConstraint.of(modality, concreteness, query.getDnf())));
	}
}
