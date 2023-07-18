/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;

public class QueryBasedRelationRewriter implements PartialRelationRewriter {
	private final Query<Boolean> may;
	private final Query<Boolean> must;
	private final Query<Boolean> candidateMay;
	private final Query<Boolean> candidateMust;

	public QueryBasedRelationRewriter(Query<Boolean> may, Query<Boolean> must, Query<Boolean> candidateMay,
									  Query<Boolean> candidateMust) {
		this.may = may;
		this.must = must;
		this.candidateMay = candidateMay;
		this.candidateMust = candidateMust;
	}

	@Override
	public List<Literal> rewriteLiteral(AbstractCallLiteral literal, Modality modality, Concreteness concreteness) {
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
