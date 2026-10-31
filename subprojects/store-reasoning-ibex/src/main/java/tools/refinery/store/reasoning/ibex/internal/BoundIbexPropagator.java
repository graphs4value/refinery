/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex.internal;

import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelListener;
import tools.refinery.store.reasoning.ibex.IbexPropagator;
import tools.refinery.store.reasoning.ibex.internal.solver.IbexSolver;

import java.util.Collection;
import java.util.List;

public class BoundIbexPropagator implements BoundPropagator, ModelListener {
	private final List<IbexSolver> solvers;

	public BoundIbexPropagator(IbexPropagator propagator, Model model, Collection<PreparedIbexRule> rules,
							   double relativeEpsilon) {
		solvers = rules.stream()
				.map(rule -> new IbexSolver(propagator, rule, relativeEpsilon, model))
				.toList();
		model.addListener(this);
	}

	@Override
	public PropagationResult propagateOne() {
		var overall = PropagationResult.UNCHANGED;
		for (var solver : solvers) {
			overall = overall.andThen(solver.propagate());
			if (overall.isRejected()) {
				return overall;
			}
		}
		return overall;
	}

	@Override
	public void afterRestore() {
		for (var solver : solvers) {
			solver.markChanged();
		}
	}

	@Override
	public void beforeClose() {
		solvers.forEach(IbexSolver::close);
	}
}
