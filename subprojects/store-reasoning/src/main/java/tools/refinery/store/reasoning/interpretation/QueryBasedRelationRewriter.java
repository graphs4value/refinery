/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

public class QueryBasedRelationRewriter extends TargetRewriter {
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
	protected Constraint getTarget(Modality modality, Concreteness concreteness) {
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
		return query.getDnf();
	}
}
