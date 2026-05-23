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

/**
 * Model-bound propagator that drives IBEX interval contraction for all rules registered with
 * the parent {@link IbexPropagator}.
 * <p>
 * One {@link IbexSolver} is created per rule per model. Concretization is a no-op: IBEX only
 * narrows intervals during the propagation stage and never picks concrete point values.
 */
public class BoundIbexPropagator implements BoundPropagator, ModelListener {
	private final List<IbexSolver> solvers;

	public BoundIbexPropagator(IbexPropagator propagator, Model model,
							   Collection<PreparedIbexRule> rules) {
		solvers = rules.stream()
				.map(rule -> new IbexSolver(propagator, rule, model))
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

	/** IBEX does not concretize; always report that concretization is not needed. */
	@Override
	public boolean concretizationRequested() {
		return false;
	}

	@Override
	public PropagationResult concretizeOne() {
		return PropagationResult.UNCHANGED;
	}

	@Override
	public PropagationResult checkConcretization() {
		return PropagationResult.UNCHANGED;
	}

	/** After a state restore (backtrack), all solvers must re-run because intervals may have changed. */
	@Override
	public void afterRestore() {
		for (var solver : solvers) {
			solver.markChanged();
		}
	}

	@Override
	public void beforeClose() {
		// The Ibex native object lives in PreparedIbexRule (shared across models) and is not
		// closed here. Call Ibex.release() on the PreparedIbexRule when the store itself is torn down.
	}
}