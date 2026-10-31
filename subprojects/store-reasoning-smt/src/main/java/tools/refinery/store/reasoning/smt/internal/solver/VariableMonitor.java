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

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class VariableMonitor {
	private final RuleBasedSolver ruleBasedSolver;
	private final Collection<AnyPartialFunction> partialFunctionsToTrack;
	private Map<AnyPartialFunction, PartialFunctionMonitor<?, ?>> monitorMap;

	public VariableMonitor(RuleBasedSolver ruleBasedSolver, Collection<AnyPartialFunction> partialFunctionsToTrack) {
		this.ruleBasedSolver = ruleBasedSolver;
		this.partialFunctionsToTrack = partialFunctionsToTrack;
	}

	public void start() {
		monitorMap = partialFunctionsToTrack.stream()
				.collect(Collectors.toUnmodifiableMap(Function.identity(),
						partialFunction -> new PartialFunctionMonitor<>(
								ruleBasedSolver, (PartialFunction<?, ?>) partialFunction)));
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
		if (monitorMap == null) {
			throw new IllegalStateException("VariableMonitor hasn't been started yet");
		}
		var monitor = monitorMap.get(partialFunction);
		if (monitor == null) {
			throw new IllegalArgumentException("No variable monitor for partial function: " + partialFunction);
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
