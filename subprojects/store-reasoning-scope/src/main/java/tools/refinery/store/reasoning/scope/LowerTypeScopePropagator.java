/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.literal.CountCandidateLowerBoundLiteral;
import tools.refinery.store.reasoning.literal.CountUpperBoundLiteral;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.Collection;
import java.util.List;

import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.term.int_.IntTerms.*;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

class LowerTypeScopePropagator extends TypeScopePropagator {
	private final int lowerBound;

	private LowerTypeScopePropagator(BoundScopePropagator adapter, int lowerBound, RelationalQuery allQuery,
									 RelationalQuery multiQuery) {
		super(adapter, allQuery, multiQuery);
		this.lowerBound = lowerBound;
	}

	@Override
	protected void doUpdateBounds() {
		constraint.setLb((lowerBound - getSingleCount()));
	}

	static class Factory extends TypeScopePropagator.Factory {
		private final PartialRelation type;
		private final int lowerBound;
		private final RelationalQuery allMay;
		private final RelationalQuery multiMay;

		public Factory(PartialRelation type, int lowerBound) {
			this.type = type;
			this.lowerBound = lowerBound;
			allMay = Query.of(type.name() + "#may", (builder, instance) -> builder.clause(
					may(type.call(instance))
			));
			multiMay = Query.of(type.name() + "#multiMay", (builder, instance) -> builder.clause(
					may(type.call(instance)),
					MULTI_VIEW.call(instance)
			));
		}

		@Override
		public TypeScopePropagator createPropagator(BoundScopePropagator adapter) {
			return new LowerTypeScopePropagator(adapter, lowerBound, allMay, multiMay);
		}

		@Override
		protected Collection<AnyQuery> getQueries() {
			return List.of(allMay, multiMay);
		}

		@Override
		public void configure(ModelStoreBuilder storeBuilder) {
			super.configure(storeBuilder);

			var requiredObjects = Query.of(type.name() + "#required", Integer.class, (builder, output) -> builder
					.clause(Integer.class, candidateLowerBound -> List.of(
							new CountCandidateLowerBoundLiteral(candidateLowerBound, type, List.of(Variable.of())),
							output.assign(sub(constant(lowerBound), candidateLowerBound)),
							check(greater(output, constant(0)))
					)));
			var tooFewObjects = Query.of(type.name() + "#tooFew", builder -> builder
					.clause(UpperCardinality.class, upperBound -> List.of(
							new CountUpperBoundLiteral(upperBound, type, List.of(Variable.of())),
							check(UpperCardinalityTerms.less(upperBound,
									UpperCardinalityTerms.constant(UpperCardinality.of(lowerBound))))
					)));

			storeBuilder.getAdapter(ReasoningBuilder.class).objective(Objectives.value(requiredObjects));
			storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class).ifPresent(dseBuilder -> {
                dseBuilder.accept(Criteria.whenNoMatch(requiredObjects));
				dseBuilder.exclude(Criteria.whenHasMatch(tooFewObjects));
            });
		}
	}
}
