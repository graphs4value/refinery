/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl.rule;

import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;

import java.util.List;

public class BoundRuleBasedPropagator implements BoundPropagator {
	private final ModelQueryAdapter queryEngine;
	private final BoundPropagationRule[] boundRules;

	public BoundRuleBasedPropagator(Model model, List<Rule> propagationRules) {
		queryEngine = model.getAdapter(ModelQueryAdapter.class);
		boundRules = new BoundPropagationRule[propagationRules.size()];
		for (int i = 0; i < boundRules.length; i++) {
			boundRules[i] = new BoundPropagationRule(model, propagationRules.get(i));
		}
	}

	@Override
	public PropagationResult propagateOne() {
		queryEngine.flushChanges();
		PropagationResult result = PropagationResult.UNCHANGED;
		// Use a classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < boundRules.length; i++) {
			var lastResult = boundRules[i].fireAll();
			result = result.andThen(lastResult);
			if (result.isRejected()) {
				break;
			}
		}
		return result;
	}
}
