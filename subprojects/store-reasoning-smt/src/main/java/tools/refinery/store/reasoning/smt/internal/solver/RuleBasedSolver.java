/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.reasoning.smt.internal.PreparedSmtRule;
import tools.refinery.store.reasoning.smt.internal.context.ModelContext;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class RuleBasedSolver {
	public static final String REJECTION_UNSAT = "SMT problem is not satisfiable";
	public static final String REJECTION_UNKNOWN = "SMT solver failed to return model";
	public static final String REJECTION_NO_MODEL = REJECTION_UNKNOWN + " unexpectedly";

	private final ModelContext context;
	private final Concreteness concreteness;
	private final Solver solver;
	private final VariableMonitor variableMonitor;
	private final List<RuleMonitor> ruleMonitors;
	private boolean started;
	private boolean changed = true;
	private PropagationResult cachedResult;

	public RuleBasedSolver(ModelContext context, Concreteness concreteness, List<PreparedSmtRule> rules,
						   Solver solver) {
		this.context = context;
		this.concreteness = concreteness;
		this.solver = solver;
		solver.interrupt();
		variableMonitor = new VariableMonitor(this);
		ruleMonitors = rules.stream()
				.map(rule -> new RuleMonitor(this, rule))
				.toList();
	}

	public ModelContext getContext() {
		return context;
	}

	public Concreteness getConcreteness() {
		return concreteness;
	}

	public boolean isChanged() {
		return changed;
	}

	public void markChanged() {
		changed = true;
	}

	private void markUnchanged() {
		changed = false;
	}

	private boolean canReuseResult() {
		return !isChanged() && cachedResult != null && cachedResult != PropagationResult.PROPAGATED;
	}

	public void changeRef(AnyPartialFunction partialFunction, Tuple tuple, boolean add) {
		variableMonitor.changeRef(partialFunction, tuple, add);
	}

	private Status doCheckSatisfiable() {
		startIfNeeded();
		solver.reset();
		variableMonitor.addAssertions(solver);
		for (var ruleMonitor : ruleMonitors) {
			ruleMonitor.addAssertions(solver);
		}
		return context.callWithInterrupt(solver::check);
	}

	private void startIfNeeded() {
		if (started) {
			return;
		}
		for (var rule : ruleMonitors) {
			rule.start();
		}
		started = true;
	}

	public PropagationResult checkSatisfiable(Object reason) {
		if (canReuseResult()) {
			return cachedResult;
		}
		var status = doCheckSatisfiable();
		cachedResult = switch (status) {
			case SATISFIABLE, UNKNOWN -> PropagationResult.UNCHANGED;
			case UNSATISFIABLE -> new PropagationRejectedResult(reason, REJECTION_UNSAT);
		};
		markUnchanged();
		return cachedResult;
	}

	public PropagationResult concretize(Object reason) {
		if (canReuseResult()) {
			return cachedResult;
		}
		var status = doCheckSatisfiable();
		cachedResult = switch (status) {
			case SATISFIABLE -> {
				if (!variableMonitor.isTracking()) {
					yield PropagationResult.UNCHANGED;
				}
				var model = context.callWithInterrupt(solver::getModel);
				if (model == null) {
					yield new PropagationRejectedResult(reason, REJECTION_NO_MODEL, true);
				}
				yield variableMonitor.refineWithModel(model, reason);
			}
			case UNKNOWN -> new PropagationRejectedResult(reason, REJECTION_UNKNOWN);
			case UNSATISFIABLE -> new PropagationRejectedResult(reason, REJECTION_UNSAT);
		};
		markUnchanged();
		return cachedResult;
	}
}
