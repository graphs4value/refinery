/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;
import java.util.Set;

public class QueryBasedRelationRewriter implements PartialRelationRewriter {
	private final RelationalQuery may;
	private final RelationalQuery must;
	private final RelationalQuery candidateMay;
	private final RelationalQuery candidateMust;

	public QueryBasedRelationRewriter(RelationalQuery may, RelationalQuery must, RelationalQuery candidateMay,
									  RelationalQuery candidateMust) {
		this.may = may;
		this.must = must;
		this.candidateMay = candidateMay;
		this.candidateMust = candidateMust;
	}

	public RelationalQuery getMay() {
		return may;
	}

	public RelationalQuery getMust() {
		return must;
	}

	public RelationalQuery getCandidateMay() {
		return candidateMay;
	}

	public RelationalQuery getCandidateMust() {
		return candidateMust;
	}

	@Override
	public List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal,
										Modality modality, Concreteness concreteness) {
		var query = switch (concreteness) {
			case PARTIAL -> switch (modality) {
				case MAY -> may;
				case MUST -> must;
			};
			case CANDIDATE -> switch (modality) {
				case MAY -> candidateMay;
				case MUST -> candidateMust;
			};
		};
		return List.of(literal.withTarget(query.getDnf()));
	}
}
