/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.localsearch;

import tools.refinery.interpreter.localsearch.planner.cost.IConstraintEvaluationContext;
import tools.refinery.interpreter.localsearch.planner.cost.impl.StatisticsBasedConstraintCostFunction;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.TypeConstraint;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Accuracy;

import java.util.Optional;

public class FlatCostFunction extends StatisticsBasedConstraintCostFunction {
	public FlatCostFunction() {
		// No inverse navigation penalty thanks to relational storage.
		super(0);
	}

	@Override
	public Optional<Long> projectionSize(IConstraintEvaluationContext input, IInputKey supplierKey, TupleMask groupMask, Accuracy requiredAccuracy) {
		// We always start from an empty model, where every projection is of size 0.
		// Therefore, projection size estimation is meaningless.
		return Optional.empty();
	}

	@Override
	protected double _calculateCost(TypeConstraint constraint, IConstraintEvaluationContext input) {
		// Assume a flat cost for each relation. Maybe adjust in the future if we perform indexing?
		return DEFAULT_COST;
	}
}
