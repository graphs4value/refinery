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
	private final BoundPropagationRule[] boundPropagationRules;
	private final BoundPropagationRule[] boundConcretizationRules;

	public BoundRuleBasedPropagator(Model model, List<Rule> propagationRules, List<Rule> concretizationRules) {
		queryEngine = model.getAdapter(ModelQueryAdapter.class);
		boundPropagationRules = bindAll(model, propagationRules);
		boundConcretizationRules = bindAll(model, concretizationRules);
	}

	private static BoundPropagationRule[] bindAll(Model model, List<Rule> rules) {
		var boundRules = new BoundPropagationRule[rules.size()];
		for (int i = 0; i < boundRules.length; i++) {
			boundRules[i] = new BoundPropagationRule(model, rules.get(i));
		}
		return boundRules;
	}

	@Override
	public PropagationResult propagateOne() {
		return fireAll(boundPropagationRules);
	}

	@Override
	public boolean concretizationRequested() {
		queryEngine.flushChanges();
		// Use a classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < boundConcretizationRules.length; i++) {
			if (boundConcretizationRules[i].canFire()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public PropagationResult concretizeOne() {
		return fireAll(boundConcretizationRules);
	}

	private PropagationResult fireAll(BoundPropagationRule[] boundRules) {
		queryEngine.flushChanges();
		boolean hasStaticRules = false;
		PropagationResult result = PropagationResult.UNCHANGED;
		// Use a classic for loop to avoid allocating an iterator.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < boundRules.length; i++) {
			var boundRule = boundRules[i];
			if (!boundRule.isDynamic()) {
				hasStaticRules = true;
			}
			var lastResult = boundRule.fireAll();
			result = result.andThen(lastResult);
			if (result.isRejected()) {
				break;
			}
		}
		if (!hasStaticRules || !result.isChanged()) {
			return result;
		}
		PropagationResult roundResult;
		do {
			queryEngine.flushChanges();
			roundResult = PropagationResult.UNCHANGED;
			// Use a classic for loop to avoid allocating an iterator.
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < boundRules.length; i++) {
				var boundRule = boundRules[i];
				if (boundRule.isDynamic()) {
					// Dynamic rules may get stuck in a loop, so pass the control back to other propagators to allow
					// them to detect inconsistency and abort the loop.
					continue;
				}
				var lastResult = boundRule.fireAll();
				roundResult = roundResult.andThen(lastResult);
				if (roundResult.isRejected()) {
					break;
				}
			}
			result = result.andThen(roundResult);
		} while (roundResult.isChanged());
		return result;
	}
}
