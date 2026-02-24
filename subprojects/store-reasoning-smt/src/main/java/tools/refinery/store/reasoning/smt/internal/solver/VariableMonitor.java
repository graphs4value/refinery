/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.tuple.Tuple;

import java.util.HashMap;
import java.util.Map;

class VariableMonitor {
	private final RuleBasedSolver ruleBasedSolver;
	private final Map<AnyPartialFunction, PartialFunctionMonitor<?, ?>> monitorMap = new HashMap<>();

	public VariableMonitor(RuleBasedSolver ruleBasedSolver) {
		this.ruleBasedSolver = ruleBasedSolver;
	}

	public void changeRef(AnyPartialFunction partialFunction, Tuple tuple, boolean add) {
		var monitor = getMonitor(partialFunction);
		if (add) {
			monitor.addRef(tuple);
			return;
		}
		monitor.removeRef(tuple);
	}

	private PartialFunctionMonitor<?, ?> getMonitor(AnyPartialFunction partialFunction) {
		var monitor = monitorMap.get(partialFunction);
		if (monitor == null) {
			monitor = new PartialFunctionMonitor<>(ruleBasedSolver, (PartialFunction<?, ?>) partialFunction);
			monitorMap.put(partialFunction, monitor);
		}
		return monitor;
	}

	public void addAssertions(Solver solver) {
		for (var entry : monitorMap.entrySet()) {
			entry.getValue().addAssertions(solver);
		}
	}
	public boolean isTracking() {
		for (var monitor : monitorMap.values()) {
			if (monitor.isTracking()) {
				return true;
			}
		}
		return false;
	}

	public PropagationResult refineWithModel(Model model, Object reason) {
		var result = PropagationResult.UNCHANGED;
		for (var monitor : monitorMap.values()) {
			result = result.andThen(monitor.refineWithModel(model, reason));
			if (result.isRejected()) {
				break;
			}
		}
		return result;
	}
}
