/*******************************************************************************
 * Copyright (c) 2010-2014, Miklos Foldenyi, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.dse.internal;

import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.dse.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.Strategy;
import tools.refinery.store.dse.objectives.Fitness;
import tools.refinery.store.dse.objectives.Objective;
import tools.refinery.store.dse.objectives.ObjectiveComparatorHelper;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;
import tools.refinery.visualization.ModelVisualizerAdapter;

import java.util.*;

public class DesignSpaceExplorationAdapterImpl implements DesignSpaceExplorationAdapter {
	static final Symbol<Integer> NODE_COUNT_SYMBOL = Symbol.of("MODEL_SIZE", 0, Integer.class, 0);
	private final Model model;
	private final ModelQueryAdapter queryEngine;
	private final DesignSpaceExplorationStoreAdapterImpl storeAdapter;
	private final Set<TransformationRule> transformationRules;
	private final Set<RelationalQuery> globalConstraints;
	private final List<Objective> objectives;
	private final LinkedHashSet<ResultSet<Boolean>> globalConstraintResultSets = new LinkedHashSet<>();
	private final Interpretation<Integer> sizeInterpretation;
	private final Strategy strategy;

	private ObjectiveComparatorHelper objectiveComparatorHelper;
	private List<Version> trajectory = new ArrayList<>();
	private Map<Version, Version> parents = new HashMap<>();
	private final List<Version> solutions = new ArrayList<>();
	private Map<Version, List<Activation>> statesAndTraversedActivations;
	@SuppressWarnings("squid:S2245")
	private Random random = new Random();
	private boolean isNewState = false;
	private final boolean isVisualizationEnabled;
	private final ModelVisualizerAdapter modelVisualizerAdapter;

	private final Map<Version, Fitness> fitnessCache = new HashMap<>();

	public DesignSpaceExplorationAdapterImpl(Model model, DesignSpaceExplorationStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		this.sizeInterpretation = model.getInterpretation(NODE_COUNT_SYMBOL);
		queryEngine = model.getAdapter(ModelQueryAdapter.class);

		globalConstraints = storeAdapter.getGlobalConstraints();
		for (var constraint : globalConstraints) {
			globalConstraintResultSets.add(queryEngine.getResultSet(constraint));
		}

		transformationRules = storeAdapter.getTransformationSpecifications();
		for (var rule : transformationRules) {
			rule.prepare(model, queryEngine);
		}

		objectives = storeAdapter.getObjectives();
		statesAndTraversedActivations = new HashMap<>();
		strategy = storeAdapter.getStrategy();
		strategy.initialize(this);
		modelVisualizerAdapter = model.tryGetAdapter(ModelVisualizerAdapter.class).orElse(null);
		isVisualizationEnabled = modelVisualizerAdapter != null;

	}

	@Override
	public void addTransformationRule(TransformationRule rule) {
		transformationRules.add(rule);
		rule.prepare(model, queryEngine);
	}

	public List<Version> getTrajectory() {
		return new ArrayList<>(trajectory);
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public DesignSpaceExplorationStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public List<Version> explore() {
		var state = model.commit();
		trajectory.add(state);
		strategy.explore();
		if (isVisualizationEnabled) {
			modelVisualizerAdapter.visualize();
		}
		return solutions;
	}

	@Override
	public int getModelSize() {
		return sizeInterpretation.get(Tuple.of());
	}

	@Override
	public Tuple1 createObject() {
		var newNodeId =  getModelSize();
		sizeInterpretation.put(Tuple.of(), newNodeId + 1);
		return Tuple.of(newNodeId);
	}

	@Override
	public Tuple deleteObject(Tuple tuple) {
		if (tuple.getSize() != 1) {
			throw new IllegalArgumentException("Tuple size must be 1");
		}
//		TODO: implement more efficient deletion
//		if (tuple.get(0) == getModelSize() - 1) {
//			sizeInterpretation.put(Tuple.of(), getModelSize() - 1);
//		}
		return tuple;
	}

	@Override
	public boolean checkGlobalConstraints() {
		for (var resultSet : globalConstraintResultSets) {
			if (resultSet.size() > 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean backtrack() {
		return backtrack("");
	}
	@Override
	public boolean backtrack(String reason) {
		if (trajectory.size() < 2) {
			return false;
		}
		var currentState = model.getState();
		if (!parents.containsKey(currentState)) {
			return false;
		}
		if (isVisualizationEnabled) {
			modelVisualizerAdapter.addTransition(trajectory.get(trajectory.size() - 1),
					trajectory.get(trajectory.size() - 2), "backtrack(" + reason + ")");
		}
		model.restore(parents.get(model.getState()));
		trajectory.remove(trajectory.size() - 1);
		return true;
	}

	@Override
	public void restoreTrajectory(List<Version> trajectory) {
		model.restore(trajectory.get(trajectory.size() - 1));
//		if (isVisualizationEnabled) {
//			modelVisualizerAdapter.addTransition(this.trajectory.get(trajectory.size() - 1),
//					trajectory.get(trajectory.size() - 1), "restore");
//		}
		this.trajectory = new ArrayList<>(trajectory);

	}

	@Override
	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	@SuppressWarnings("squid:S2245")
	public void setRandom(long seed) {
		this.random = new Random(seed);
	}

	@Override
	public List<Version> getSolutions() {
		return solutions;
	}

	@Override
	public Fitness getFitness() {
        return fitnessCache.computeIfAbsent(model.getState(), s -> calculateFitness());
	}

	private Fitness calculateFitness() {
		Fitness result = new Fitness();
		boolean satisfiesHardObjectives = true;
		for (Objective objective : objectives) {
			var fitness = objective.getFitness(this);
			result.put(objective.getName(), fitness);
			if (objective.isHardObjective() && !objective.satisfiesHardObjective(fitness)) {
				satisfiesHardObjectives = false;
			}
		}
		result.setSatisfiesHardObjectives(satisfiesHardObjectives);

		return result;
	}

	@Override
	public void newSolution() {
		var state = model.getState();
		solutions.add(state);
		if (isVisualizationEnabled) {
			modelVisualizerAdapter.addSolution(state);
		}
	}

	@Override
	public int getDepth() {
		return trajectory.size() - 1;
	}

	public LinkedHashSet<Activation> getUntraversedActivations() {
		var traversedActivations = statesAndTraversedActivations.get(model.getState());
		if (traversedActivations == null) {
			return new LinkedHashSet<>(getAllActivations());
		}
		else {
			LinkedHashSet<Activation> untraversedActivations = new LinkedHashSet<>();
			for (Activation activation : getAllActivations()) {
				if (!traversedActivations.contains(activation)) {
					untraversedActivations.add(activation);
				}
			}
			return untraversedActivations;
		}
	}

	@Override
	public boolean fireActivation(Activation activation) {
		if (activation == null) {
			return false;
		}
		var previousState = model.getState();
		if (!activation.fire()) {
			return false;
		}
		statesAndTraversedActivations.computeIfAbsent(previousState, s -> new ArrayList<>()).add(activation);
		var newState = model.commit();
		trajectory.add(newState);
		parents.put(newState, previousState);
		isNewState = !statesAndTraversedActivations.containsKey(newState);
		if (isVisualizationEnabled) {
			if (isNewState) {
				modelVisualizerAdapter.addState(newState, getFitness().values());
			}
			modelVisualizerAdapter.addTransition(previousState, newState, activation.transformationRule().getName(),
					activation.activation());
		}
		return true;
	}

	@Override
	public boolean fireRandomActivation() {
		var activations = getUntraversedActivations();
		if (activations.isEmpty()) {
			return false;
		}
		int index = random.nextInt(activations.size());
		var iterator = activations.iterator();
		while (index-- > 0) {
			iterator.next();
		}
		var activationId = iterator.next();
		return fireActivation(activationId);
	}

	public List<Activation> getAllActivations() {
		List<Activation> result = new LinkedList<>();
		for (var rule : transformationRules) {
			result.addAll(rule.getAllActivationsAsList());
		}
		return result;
	}

	public boolean isCurrentStateAlreadyTraversed() {
		return !isNewState;
	}

	public ObjectiveComparatorHelper getObjectiveComparatorHelper() {
		if (objectiveComparatorHelper == null) {
			objectiveComparatorHelper = new ObjectiveComparatorHelper(objectives);
		}
		return objectiveComparatorHelper;
	}
}
