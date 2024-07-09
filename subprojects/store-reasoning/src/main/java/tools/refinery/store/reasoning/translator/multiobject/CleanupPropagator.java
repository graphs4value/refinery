/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.propagation.Propagator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.ReasoningAdapter;

import java.util.List;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.UPPER_CARDINALITY_VIEW;

/**
 * Clean up non-existing objects in a separate propagator to avoid interference with propagation rules that assume
 * that each object in their activation set is still part of the model when they are called.
 */
public class CleanupPropagator implements Propagator {
	private static final RelationalQuery CLEANUP_QUERY = Query.of("exists#cleanup", (builder, node) -> builder
			.clause(UpperCardinality.class, upper -> List.of(
					UPPER_CARDINALITY_VIEW.call(node, upper),
					check(UpperCardinalityTerms.less(upper,
							UpperCardinalityTerms.constant(UpperCardinalities.ONE)))
			))
	);

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		storeBuilder.getAdapter(ModelQueryBuilder.class).query(CLEANUP_QUERY);
	}

	@Override
	public BoundPropagator bindToModel(Model model) {
		return new BoundCleanupPropagator(model);
	}

	private class BoundCleanupPropagator implements BoundPropagator {
		private final Model model;
		private final ModelQueryAdapter queryEngine;
		private final ResultSet<Boolean> resultSet;
		private ReasoningAdapter reasoningAdapter;

		public BoundCleanupPropagator(Model model) {
			this.model = model;
			queryEngine = model.getAdapter(ModelQueryAdapter.class);
			resultSet = queryEngine.getResultSet(CLEANUP_QUERY);
		}

		@Override
		public PropagationResult propagateOne() {
			if (reasoningAdapter == null) {
				reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			}
			queryEngine.flushChanges();
			boolean propagated = false;
			var cursor = resultSet.getAll();
			while (cursor.move()) {
				propagated = true;
				var nodeToDelete = cursor.getKey().get(0);
				if (!reasoningAdapter.cleanup(nodeToDelete)) {
					return new PropagationRejectedResult(CleanupPropagator.this,
                            "Failed to remove node: " + nodeToDelete, true);
				}
			}
			return propagated ? PropagationResult.PROPAGATED : PropagationResult.UNCHANGED;
		}
	}
}
