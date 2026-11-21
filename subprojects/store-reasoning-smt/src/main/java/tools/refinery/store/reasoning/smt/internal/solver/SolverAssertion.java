/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.BoolSort;
import com.microsoft.z3.Expr;
import tools.refinery.logic.AbstractValue;

import java.util.List;

public record SolverAssertion(Object source, AbstractValue<?, ?> value, List<Expr<BoolSort>> expressions) {
	public SolverAssertion {
		expressions = List.copyOf(expressions);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	boolean isImpliedBy(SolverAssertion current) {
		if (current == null) {
			return false;
		}
		if (value == null) {
			return equals(current);
		}
		return current.value() != null && ((AbstractValue) current.value()).isRefinementOf(value);
	}
}
