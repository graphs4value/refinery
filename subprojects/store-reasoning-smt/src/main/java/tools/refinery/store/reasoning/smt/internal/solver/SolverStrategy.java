/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.Status;

import java.util.Map;
import java.util.Set;

public interface SolverStrategy {
	/**
	 * Synchronizes read-only assertions and changed source keys, including removals absent from the map.
	 * After reset, changedSources must contain every current source key.
	 */
	void synchronize(Map<Object, SolverAssertion> assertions, Set<Object> changedSources);

	/** Checks the synchronized assertions, leaving the resulting model available on the solver. */
	Status check();

	/** Clears solver assertions and strategy history so the journal can replay all current sources. */
	void reset();
}
