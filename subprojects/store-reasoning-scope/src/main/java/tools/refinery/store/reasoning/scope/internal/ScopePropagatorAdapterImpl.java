/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope.internal;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.reasoning.refinement.RefinementResult;
import tools.refinery.store.reasoning.scope.ScopePropagatorAdapter;
import tools.refinery.store.reasoning.scope.ScopePropagatorStoreAdapter;
import tools.refinery.store.representation.cardinality.*;
import tools.refinery.store.tuple.Tuple;

class ScopePropagatorAdapterImpl implements ScopePropagatorAdapter {
	private final Model model;
	private final ScopePropagatorStoreAdapterImpl storeAdapter;
	private final ModelQueryAdapter queryEngine;
	private final Interpretation<CardinalityInterval> countInterpretation;
	private final MPSolver solver;
	private final MPObjective objective;
	private final MutableIntObjectMap<MPVariable> variables = IntObjectMaps.mutable.empty();
	private final MutableIntSet activeVariables = IntSets.mutable.empty();
	private final TypeScopePropagator[] propagators;
	private boolean changed = true;

	public ScopePropagatorAdapterImpl(Model model, ScopePropagatorStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		queryEngine = model.getAdapter(ModelQueryAdapter.class);
		countInterpretation = model.getInterpretation(storeAdapter.getCountSymbol());
		solver = MPSolver.createSolver("GLOP");
		objective = solver.objective();
		initializeVariables();
		countInterpretation.addListener(this::countChanged, true);
		var propagatorFactories = storeAdapter.getPropagatorFactories();
		propagators = new TypeScopePropagator[propagatorFactories.size()];
		for (int i = 0; i < propagators.length; i++) {
			propagators[i] = propagatorFactories.get(i).createPropagator(this);
		}
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ScopePropagatorStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	private void initializeVariables() {
		var cursor = countInterpretation.getAll();
		while (cursor.move()) {
			var interval = cursor.getValue();
			if (!interval.equals(CardinalityIntervals.ONE)) {
				int nodeId = cursor.getKey().get(0);
				createVariable(nodeId, interval);
				activeVariables.add(nodeId);
			}
		}
	}

	private MPVariable createVariable(int nodeId, CardinalityInterval interval) {
		double lowerBound = interval.lowerBound();
		double upperBound = getUpperBound(interval);
		var variable = solver.makeNumVar(lowerBound, upperBound, "x" + nodeId);
		variables.put(nodeId, variable);
		return variable;
	}

	private void countChanged(Tuple key, CardinalityInterval fromValue, CardinalityInterval toValue,
							  boolean ignoredRestoring) {
		int nodeId = key.get(0);
		if ((toValue == null || toValue.equals(CardinalityIntervals.ONE))) {
			if (fromValue != null && !fromValue.equals(CardinalityIntervals.ONE)) {
				var variable = variables.get(nodeId);
				if (variable == null || !activeVariables.remove(nodeId)) {
					throw new AssertionError("Variable not active: " + nodeId);
				}
				variable.setBounds(0, 0);
				markAsChanged();
			}
			return;
		}
		if (fromValue == null || fromValue.equals(CardinalityIntervals.ONE)) {
			activeVariables.add(nodeId);
		}
		var variable = variables.get(nodeId);
		if (variable == null) {
			createVariable(nodeId, toValue);
			markAsChanged();
			return;
		}
		double lowerBound = toValue.lowerBound();
		double upperBound = getUpperBound(toValue);
		if (variable.lb() != lowerBound) {
			variable.setLb(lowerBound);
			markAsChanged();
		}
		if (variable.ub() != upperBound) {
			variable.setUb(upperBound);
			markAsChanged();
		}
	}

	MPConstraint makeConstraint() {
		return solver.makeConstraint();
	}

	MPVariable getVariable(int nodeId) {
		var variable = variables.get(nodeId);
		if (variable != null) {
			return variable;
		}
		var interval = countInterpretation.get(Tuple.of(nodeId));
		if (interval == null || interval.equals(CardinalityIntervals.ONE)) {
			interval = CardinalityIntervals.NONE;
		} else {
			activeVariables.add(nodeId);
			markAsChanged();
		}
		return createVariable(nodeId, interval);
	}

	void markAsChanged() {
		changed = true;
	}

	@Override
	public RefinementResult propagate() {
		var result = RefinementResult.UNCHANGED;
		RefinementResult currentRoundResult;
		do {
			currentRoundResult = propagateOne();
			result = result.andThen(currentRoundResult);
			if (result.isRejected()) {
				return result;
			}
		} while (currentRoundResult != RefinementResult.UNCHANGED);
		return result;
	}

	private RefinementResult propagateOne() {
		queryEngine.flushChanges();
		if (!changed) {
			return RefinementResult.UNCHANGED;
		}
		changed = false;
		for (var propagator : propagators) {
			propagator.updateBounds();
		}
		var result = RefinementResult.UNCHANGED;
		if (activeVariables.isEmpty()) {
			return checkEmptiness();
		}
		var iterator = activeVariables.intIterator();
		while (iterator.hasNext()) {
			int nodeId = iterator.next();
			var variable = variables.get(nodeId);
			if (variable == null) {
				throw new AssertionError("Missing active variable: " + nodeId);
			}
			result = result.andThen(propagateNode(nodeId, variable));
			if (result.isRejected()) {
				return result;
			}
		}
		return result;
	}

	private RefinementResult checkEmptiness() {
		var emptinessCheckingResult = solver.solve();
		return switch (emptinessCheckingResult) {
			case OPTIMAL, UNBOUNDED -> RefinementResult.UNCHANGED;
			case INFEASIBLE -> RefinementResult.REJECTED;
			default -> throw new IllegalStateException("Failed to check for consistency: " + emptinessCheckingResult);
		};
	}

	private RefinementResult propagateNode(int nodeId, MPVariable variable) {
		objective.setCoefficient(variable, 1);
		try {
			objective.setMinimization();
			var minimizationResult = solver.solve();
			int lowerBound;
			switch (minimizationResult) {
			case OPTIMAL -> lowerBound = RoundingUtil.roundUp(objective.value());
			case UNBOUNDED -> lowerBound = 0;
			case INFEASIBLE -> {
				return RefinementResult.REJECTED;
			}
			default -> throw new IllegalStateException("Failed to solve for minimum of %s: %s"
					.formatted(variable, minimizationResult));
			}

			objective.setMaximization();
			var maximizationResult = solver.solve();
			UpperCardinality upperBound;
			switch (maximizationResult) {
			case OPTIMAL -> upperBound = UpperCardinalities.atMost(RoundingUtil.roundDown(objective.value()));
			// Problem was feasible when minimizing, the only possible source of {@code UNBOUNDED_OR_INFEASIBLE} is
			// an unbounded maximization problem. See https://github.com/google/or-tools/issues/3319
			case UNBOUNDED, INFEASIBLE -> upperBound = UpperCardinalities.UNBOUNDED;
			default -> throw new IllegalStateException("Failed to solve for maximum of %s: %s"
					.formatted(variable, minimizationResult));
			}

			var newInterval = CardinalityIntervals.between(lowerBound, upperBound);
			var oldInterval = countInterpretation.put(Tuple.of(nodeId), newInterval);
			if (newInterval.lowerBound() < oldInterval.lowerBound() ||
					newInterval.upperBound().compareTo(oldInterval.upperBound()) > 0) {
				throw new IllegalArgumentException("Failed to refine multiplicity %s of node %d to %s"
						.formatted(oldInterval, nodeId, newInterval));
			}
			return newInterval.equals(oldInterval) ? RefinementResult.UNCHANGED : RefinementResult.REFINED;
		} finally {
			objective.setCoefficient(variable, 0);
		}
	}

	private static double getUpperBound(CardinalityInterval interval) {
		var upperBound = interval.upperBound();
		if (upperBound instanceof FiniteUpperCardinality finiteUpperCardinality) {
			return finiteUpperCardinality.finiteUpperBound();
		} else if (upperBound instanceof UnboundedUpperCardinality) {
			return Double.POSITIVE_INFINITY;
		} else {
			throw new IllegalArgumentException("Unknown upper bound: " + upperBound);
		}
	}
}
