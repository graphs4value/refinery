/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt;

import com.microsoft.z3.Context;
import tools.refinery.z3.Z3SolverLoader;

public class SmtPropagator {
	public void propagate() {
		Z3SolverLoader.loadNativeLibraries();
		try (var context = new Context()) {
			var solver = context.mkSolver();
			solver.check();
		}
	}
}
