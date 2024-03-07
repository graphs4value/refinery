/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.CallLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.reasoning.interpretation.QueryBasedRelationRewriter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;
import java.util.Set;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.term.uppercardinality.UpperCardinalityTerms.constant;
import static tools.refinery.logic.term.uppercardinality.UpperCardinalityTerms.lessEq;

class EqualsRelationRewriter extends QueryBasedRelationRewriter {
	private EqualsRelationRewriter(RelationalQuery may, RelationalQuery must) {
		super(may, must, may, may);
	}

	@Override
	public List<Literal> rewriteLiteral(Set<Variable> positiveVariables, AbstractCallLiteral literal,
										Modality modality, Concreteness concreteness) {
		if (!(literal instanceof CallLiteral callLiteral)) {
			return super.rewriteLiteral(positiveVariables, literal, modality, concreteness);
		}
		var left = callLiteral.getArguments().get(0).asNodeVariable();
		var right = callLiteral.getArguments().get(1).asNodeVariable();
		boolean useMay = modality == Modality.MAY || concreteness == Concreteness.CANDIDATE;
		return switch (callLiteral.getPolarity()) {
			case POSITIVE, TRANSITIVE -> {
				if (useMay) {
					if (positiveVariables.contains(left) || positiveVariables.contains(right)) {
						// No need to enumerate arguments if at least one is already bound, since they will be unified.
						yield List.of(left.isEquivalent(right));
					} else {
						yield List.of(
								left.isEquivalent(right),
								getMay().call(left, right)
						);
					}
				} else {
					yield List.of(
							left.isEquivalent(right),
							getMust().call(left, right)
					);
				}
			}
			case NEGATIVE -> {
				if (useMay) {
					yield List.of(left.notEquivalent(right));
				} else {
					yield super.rewriteLiteral(positiveVariables, literal, modality, concreteness);
				}
			}
		};
	}

	public static EqualsRelationRewriter of(AnySymbolView upperCardinalityView) {
		var may = Query.of("equals#may", (builder, p1, p2) -> builder
				.clause(
						p1.isEquivalent(p2),
						upperCardinalityView.call(p1, Variable.of(UpperCardinality.class))
				));
		var must = Query.of("equals#must", (builder, p1, p2) -> builder
				.clause(UpperCardinality.class, upper -> List.of(
						p1.isEquivalent(p2),
						upperCardinalityView.call(p1, upper),
						check(lessEq(upper, constant(UpperCardinalities.ONE)))
				)));
		return new EqualsRelationRewriter(may, must);
	}
}
