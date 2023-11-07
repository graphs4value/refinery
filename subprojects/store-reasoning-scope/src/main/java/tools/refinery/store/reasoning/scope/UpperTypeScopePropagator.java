/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.literal.CountCandidateUpperBoundLiteral;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collection;
import java.util.List;

import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.greater;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

class UpperTypeScopePropagator extends TypeScopePropagator {
	private final int upperBound;

	private UpperTypeScopePropagator(BoundScopePropagator adapter, int upperBound, RelationalQuery allQuery,
									 RelationalQuery multiQuery) {
		super(adapter, allQuery, multiQuery);
		this.upperBound = upperBound;
	}

	@Override
	protected void doUpdateBounds() {
		constraint.setUb((upperBound - getSingleCount()));
	}

	static class Factory extends TypeScopePropagator.Factory {
		private final PartialRelation type;
		private final int upperBound;
		private final RelationalQuery allMust;
		private final RelationalQuery multiMust;

		public Factory(PartialRelation type, int upperBound) {
			this.type = type;
			this.upperBound = upperBound;
			allMust = Query.of(type.name() + "#must", (builder, instance) -> builder.clause(
					must(type.call(instance))
			));
			multiMust = Query.of(type.name() + "#multiMust", (builder, instance) -> builder.clause(
					must(type.call(instance)),
					MULTI_VIEW.call(instance)
			));
		}

		@Override
		public TypeScopePropagator createPropagator(BoundScopePropagator adapter) {
			return new UpperTypeScopePropagator(adapter, upperBound, allMust, multiMust);
		}

		@Override
		protected Collection<AnyQuery> getQueries() {
			return List.of(allMust, multiMust);
		}

		@Override
		public void configure(ModelStoreBuilder storeBuilder) {
			super.configure(storeBuilder);

			var tooManyObjects = Query.of(type.name() + "#tooMany", builder -> builder
					.clause(Integer.class, candidateUpperBound -> List.of(
							new CountCandidateUpperBoundLiteral(candidateUpperBound, type, List.of(Variable.of())),
							check(greater(candidateUpperBound, constant(upperBound)))
					)));

			storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class).ifPresent(dseBuilder ->
					dseBuilder.accept(Criteria.whenNoMatch(tooManyObjects)));
		}
	}
}
