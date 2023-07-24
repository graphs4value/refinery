/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.localsearch;

import org.eclipse.viatra.query.runtime.localsearch.planner.cost.IConstraintEvaluationContext;
import org.eclipse.viatra.query.runtime.localsearch.planner.cost.impl.StatisticsBasedConstraintCostFunction;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.util.Accuracy;

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
