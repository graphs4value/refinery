/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope.internal;

import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collection;
import java.util.List;

import static tools.refinery.store.reasoning.literal.PartialLiterals.may;

class LowerTypeScopePropagator extends TypeScopePropagator {
	private final int lowerBound;

	private LowerTypeScopePropagator(ScopePropagatorAdapterImpl adapter, int lowerBound, RelationalQuery allQuery,
									RelationalQuery multiQuery) {
		super(adapter, allQuery, multiQuery);
		this.lowerBound = lowerBound;
	}

	@Override
	public void updateBounds() {
		constraint.setLb(lowerBound - getSingleCount());
	}

	public static class Factory implements TypeScopePropagator.Factory {
		private final int lowerBound;
		private final RelationalQuery allMay;
		private final RelationalQuery multiMay;

		public Factory(RelationalQuery multi, PartialRelation type, int lowerBound) {
			this.lowerBound = lowerBound;
			allMay = Query.of(type.name() + "#may", (builder, instance) -> builder.clause(
					may(type.call(instance))
			));
			multiMay = Query.of(type.name() + "#multiMay", (builder, instance) -> builder.clause(
					may(type.call(instance)),
					multi.call(instance)
			));
		}

		@Override
		public TypeScopePropagator createPropagator(ScopePropagatorAdapterImpl adapter) {
			return new LowerTypeScopePropagator(adapter, lowerBound, allMay, multiMay);
		}

		@Override
		public Collection<AnyQuery> getQueries() {
			return List.of(allMay, multiMay);
		}
	}
}
