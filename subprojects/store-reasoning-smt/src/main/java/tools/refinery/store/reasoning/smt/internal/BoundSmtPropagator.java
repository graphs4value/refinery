/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal;

import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelListener;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.smt.SmtPropagator;
import tools.refinery.store.reasoning.smt.internal.context.ModelContext;
import tools.refinery.store.reasoning.smt.internal.solver.RuleBasedSolver;

import java.util.Collection;

public class BoundSmtPropagator implements BoundPropagator, ModelListener {
	private final SmtPropagator propagator;
	private final ModelContext context;
	private final RuleBasedSolver propagationSolver;
	private final RuleBasedSolver concretizationSolver;

	public BoundSmtPropagator(SmtPropagator propagator, Model model, Collection<PreparedSmtRule> rules,
							  int timeout, int rlimit) {
		this.propagator = propagator;
		context = new ModelContext(model, rules, timeout, rlimit);
		propagationSolver = context.createSolver(Concreteness.PARTIAL);
		concretizationSolver = context.createSolver(Concreteness.CANDIDATE);
	}

	@Override
	public PropagationResult propagateOne() {
		return propagationSolver.checkSatisfiable(propagator);
	}

	@Override
	public boolean concretizationRequested() {
		return concretizationSolver.isChanged();
	}

	@Override
	public PropagationResult concretizeOne() {
		return concretizationSolver.concretize(propagator);
	}

	@Override
	public PropagationResult checkConcretization() {
		return concretizationSolver.checkSatisfiable(propagator);
	}

	@Override
	public void afterRestore() {
		propagationSolver.markChanged();
		concretizationSolver.markChanged();
	}

	@Override
	public void beforeClose() {
		context.close();
	}
}
