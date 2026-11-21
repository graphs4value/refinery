/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

import java.util.Map;
import java.util.Set;

public final class ResetSolverStrategy implements SolverStrategy {
	private final Solver solver;

	public ResetSolverStrategy(Solver solver) {
		this.solver = solver;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void synchronize(Map<Object, SolverAssertion> assertions, Set<Object> changedSources) {
		solver.reset();
		for (var assertion : assertions.values()) {
			solver.add(assertion.expressions().toArray(Expr[]::new));
		}
	}

	@Override
	public Status check() {
		return solver.check();
	}

	@Override
	public void reset() {
		solver.reset();
	}
}
