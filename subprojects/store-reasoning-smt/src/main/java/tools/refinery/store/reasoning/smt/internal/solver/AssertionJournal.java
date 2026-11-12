/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.*;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.reasoning.smt.SmtSolverStrategy;

import java.util.*;

final class AssertionJournal {
	private final SolverStrategy strategy;
	private final Map<Object, SolverAssertion> desired = new LinkedHashMap<>();
	private final Set<Object> dirty = new LinkedHashSet<>();
	private final Map<Object, SolverAssertion> assertionsView = Collections.unmodifiableMap(desired);
	private final Set<Object> dirtyView = Collections.unmodifiableSet(dirty);
	private boolean rebuild;

	AssertionJournal(Context context, Solver solver, SmtSolverStrategy strategy) {
		this.strategy = strategy.create(context, solver);
	}

	void put(Object source, AbstractValue<?, ?> value, List<Expr<BoolSort>> expressions) {
		var assertion = new SolverAssertion(source, value, expressions);
		if (!assertion.equals(desired.put(source, assertion))) {
			dirty.add(source);
		}
	}

	void remove(Object source) {
		if (desired.remove(source) != null) {
			dirty.add(source);
		}
	}

	Status check() {
		try {
			if (rebuild) {
				strategy.reset();
				dirty.addAll(desired.keySet());
				rebuild = false;
			}
			strategy.synchronize(assertionsView, dirtyView);
			var status = strategy.check();
			dirty.clear();
			return status;
		} catch (RuntimeException | Error e) {
			rebuild = true;
			throw e;
		}
	}
}
