/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.dnf.SymbolicParameter;
import tools.refinery.store.query.term.NodeVariable;

import java.util.*;

public class Action {
	private final List<NodeVariable> parameters;
	private final Set<NodeVariable> localVariables;
	private final List<ActionLiteral> actionLiterals;
	private final int[] @Nullable [] inputAllocations;
	private final int[] @Nullable [] outputAllocations;

	public Action(List<NodeVariable> parameters, List<? extends ActionLiteral> actionLiterals) {
		this.parameters = List.copyOf(parameters);
		this.actionLiterals = List.copyOf(actionLiterals);
		var allocation = ObjectIntMaps.mutable.<NodeVariable>empty();
		int arity = parameters.size();
		for (int i = 0; i < arity; i++) {
			allocation.put(parameters.get(i), i);
		}
		var mutableLocalVariables = new LinkedHashSet<NodeVariable>();
		int size = actionLiterals.size();
		inputAllocations = new int[size][];
		outputAllocations = new int[size][];
		for (int i = 0; i < size; i++) {
			computeInputAllocation(i, parameters, allocation);
			computeOutputAllocation(i, mutableLocalVariables, allocation);
		}
		this.localVariables = Collections.unmodifiableSet(mutableLocalVariables);
	}

	private void computeInputAllocation(int actionIndex, List<NodeVariable> parameters,
										MutableObjectIntMap<NodeVariable> allocation) {
		var actionLiteral = actionLiterals.get(actionIndex);
		var inputVariables = actionLiteral.getInputVariables();
		if (inputVariables.equals(parameters)) {
			// Identity mappings use a {@code null} allocation to pass the activation tuple unchanged.
			return;
		}
		var inputs = new int[inputVariables.size()];
		for (int i = 0; i < inputs.length; i++) {
			var variable = inputVariables.get(i);
			if (!allocation.containsKey(variable)) {
				throw new IllegalArgumentException("Unbound input variable %s of action literal %s"
						.formatted(variable, actionLiteral));
			}
			inputs[i] = allocation.get(variable);
		}
		inputAllocations[actionIndex] = inputs;
	}

	private void computeOutputAllocation(int actionIndex, Set<NodeVariable> mutableLocalVariable,
										 MutableObjectIntMap<NodeVariable> allocation) {
		var actionLiteral = actionLiterals.get(actionIndex);
		var outputVariables = actionLiteral.getOutputVariables();
		int size = outputVariables.size();
		if (size == 0) {
			// Identity mappings use a {@code null} allocation to avoid iterating over the output tuple.
			return;
		}
		if (size >= 2 && new HashSet<>(outputVariables).size() != size) {
			throw new IllegalArgumentException("Action literal %s has duplicate output variables %s"
					.formatted(actionLiteral, outputVariables));
		}
		int arity = parameters.size();
		var outputs = new int[size];
		for (int i = 0; i < size; i++) {
			var variable = outputVariables.get(i);
			if (allocation.containsKey(variable)) {
				throw new IllegalArgumentException("Output variable %s of action literal %s was already assigned"
						.formatted(variable, actionLiteral));
			}
			int variableId = mutableLocalVariable.size();
			allocation.put(variable, arity + variableId);
			outputs[i] = variableId;
			mutableLocalVariable.add(variable);
		}
		outputAllocations[actionIndex] = outputs;
	}

	public List<NodeVariable> getParameters() {
		return parameters;
	}

	public int getArity() {
		return parameters.size();
	}

	public Set<NodeVariable> getLocalVariables() {
		return localVariables;
	}

	public List<ActionLiteral> getActionLiterals() {
		return actionLiterals;
	}

	int @Nullable [] getInputAllocation(int actionIndex) {
		return inputAllocations[actionIndex];
	}

	int @Nullable [] getOutputAllocation(int actionIndex) {
		return outputAllocations[actionIndex];
	}

	public BoundAction bindToModel(Model model) {
		return new BoundAction(this, model);
	}

	public static Action ofSymbolicParameters(List<SymbolicParameter> symbolicParameters,
											  List<? extends ActionLiteral> actionLiterals) {
		var nodeVariables = symbolicParameters.stream()
				.map(symbolicParameter -> symbolicParameter.getVariable().asNodeVariable())
				.toList();
		return new Action(nodeVariables, actionLiterals);
	}

	public static Action ofPrecondition(RelationalQuery precondition, List<? extends ActionLiteral> actionLiterals) {
		return ofSymbolicParameters(precondition.getDnf().getSymbolicParameters(), actionLiterals);
	}
}
