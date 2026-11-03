/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import tools.refinery.store.reasoning.smt.internal.solver.GuardedAssumptionsSolverStrategy;
import tools.refinery.store.reasoning.smt.internal.solver.PopReplaySolverStrategy;
import tools.refinery.store.reasoning.smt.internal.solver.ResetSolverStrategy;
import tools.refinery.store.reasoning.smt.internal.solver.SolverStrategy;

public enum SmtSolverStrategy {
	RESET {
		@Override
		public SolverStrategy create(Context context, Solver solver) {
			return new ResetSolverStrategy(solver);
		}
	},
	POP_REPLAY {
		@Override
		public SolverStrategy create(Context context, Solver solver) {
			return new PopReplaySolverStrategy(solver);
		}
	},
	GUARDED_ASSUMPTIONS {
		@Override
		public SolverStrategy create(Context context, Solver solver) {
			return new GuardedAssumptionsSolverStrategy(context, solver);
		}
	};

	/** Creates a strategy owning the assertion state of the supplied solver. */
	public abstract SolverStrategy create(Context context, Solver solver);
}
