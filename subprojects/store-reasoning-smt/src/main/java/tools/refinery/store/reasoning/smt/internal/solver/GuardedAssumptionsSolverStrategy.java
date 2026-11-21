/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.*;

import java.util.*;

public final class GuardedAssumptionsSolverStrategy implements SolverStrategy {
	private static final int HISTORY_LIMIT = 1024;
	private final Context context;
	private final Solver solver;
	private final Map<SolverAssertion, Expr<BoolSort>> guards = new HashMap<>();
	private final Map<Object, Expr<BoolSort>> activeGuards = new LinkedHashMap<>();

	public GuardedAssumptionsSolverStrategy(Context context, Solver solver) {
		this.context = context;
		this.solver = solver;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void synchronize(Map<Object, SolverAssertion> desired, Set<Object> dirty) {
		if (guards.size() > Math.max(HISTORY_LIMIT, desired.size() * 4L)) {
			reset();
			dirty = desired.keySet();
		}
		for (var source : dirty) {
			var assertion = desired.get(source);
			if (assertion == null || assertion.expressions().isEmpty()) {
				activeGuards.remove(source);
				continue;
			}
			var guard = guards.computeIfAbsent(assertion, key -> {
				var literal = context.mkFreshConst("smt", context.getBoolSort());
				for (var expression : key.expressions()) {
					solver.add(context.mkImplies(literal, expression));
				}
				return literal;
			});
			activeGuards.put(source, guard);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Status check() {
		return solver.check(activeGuards.values().toArray(Expr[]::new));
	}

	@Override
	public void reset() {
		solver.reset();
		guards.clear();
		activeGuards.clear();
	}
}
