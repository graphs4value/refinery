/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import tools.refinery.store.dse.propagation.BoundPropagator;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.representation.cardinality.*;
import tools.refinery.store.tuple.Tuple;

class BoundScopePropagator implements BoundPropagator {
	private final Model model;
	private final ModelQueryAdapter queryEngine;
	private final Interpretation<CardinalityInterval> countInterpretation;
	private final MPSolver solver;
	private final MPObjective objective;
	private final MutableIntObjectMap<MPVariable> variables = IntObjectMaps.mutable.empty();
	private final MutableIntSet activeVariables = IntSets.mutable.empty();
	private final TypeScopePropagator[] propagators;
	private boolean changed = true;

	public BoundScopePropagator(Model model, ScopePropagator storeAdapter) {
		this.model = model;
		queryEngine = model.getAdapter(ModelQueryAdapter.class);
		countInterpretation = model.getInterpretation(storeAdapter.getCountSymbol());
		solver = MPSolver.createSolver("GLOP");
		solver.suppressOutput();
		objective = solver.objective();
		initializeVariables();
		countInterpretation.addListener(this::countChanged, true);
		var propagatorFactories = storeAdapter.getTypeScopePropagatorFactories();
		propagators = new TypeScopePropagator[propagatorFactories.size()];
		for (int i = 0; i < propagators.length; i++) {
			model.checkCancelled();
			propagators[i] = propagatorFactories.get(i).createPropagator(this);
		}
	}

	ModelQueryAdapter getQueryEngine() {
		return queryEngine;
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
				removeActiveVariable(toValue, nodeId);
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

	private void removeActiveVariable(CardinalityInterval toValue, int nodeId) {
		var variable = variables.get(nodeId);
		if (variable == null || !activeVariables.remove(nodeId)) {
			throw new AssertionError("Variable not active: " + nodeId);
		}
		if (toValue == null) {
			variable.setBounds(0, 0);
		} else {
			// Until queries are flushed and the constraints can be properly updated,
			// the variable corresponding to the (previous) multi-object has to stand in for a single object.
			variable.setBounds(1, 1);
		}
		markAsChanged();
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
	public PropagationResult propagateOne() {
		queryEngine.flushChanges();
		if (!changed) {
			return PropagationResult.UNCHANGED;
		}
		changed = false;
		for (var propagator : propagators) {
			model.checkCancelled();
			if (!propagator.updateBounds()) {
				// Avoid logging GLOP error to console by checking for inconsistent constraints in advance.
				return PropagationResult.REJECTED;
			}
		}
		var result = PropagationResult.UNCHANGED;
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

	private PropagationResult checkEmptiness() {
		model.checkCancelled();
		var emptinessCheckingResult = solver.solve();
		return switch (emptinessCheckingResult) {
			case OPTIMAL, UNBOUNDED -> PropagationResult.UNCHANGED;
			case INFEASIBLE -> PropagationResult.REJECTED;
			default -> throw new IllegalStateException("Failed to check for consistency: " + emptinessCheckingResult);
		};
	}

	private PropagationResult propagateNode(int nodeId, MPVariable variable) {
		objective.setCoefficient(variable, 1);
		try {
			model.checkCancelled();
			objective.setMinimization();
			var minimizationResult = solver.solve();
			int lowerBound;
			switch (minimizationResult) {
			case OPTIMAL -> lowerBound = RoundingUtil.roundUp(objective.value());
			case UNBOUNDED -> lowerBound = 0;
			case INFEASIBLE -> {
				return PropagationResult.REJECTED;
			}
			default -> throw new IllegalStateException("Failed to solve for minimum of %s: %s"
					.formatted(variable, minimizationResult));
			}

			model.checkCancelled();
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
			return newInterval.equals(oldInterval) ? PropagationResult.UNCHANGED : PropagationResult.PROPAGATED;
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
