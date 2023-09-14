/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collection;
import java.util.List;

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

	public static class Factory extends TypeScopePropagator.Factory {
		private final int upperBound;
		private final RelationalQuery allMust;
		private final RelationalQuery multiMust;

		public Factory(PartialRelation type, int upperBound) {
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
	}
}
