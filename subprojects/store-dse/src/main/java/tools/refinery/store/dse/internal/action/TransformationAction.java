/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple2;

import java.util.*;

public class TransformationAction {
	private final List<ActionVariable> actionVariables = new ArrayList<>();
	private final List<InsertAction<?>> insertActions = new ArrayList<>();
	private final List<DeleteAction> deleteActions = new ArrayList<>();
	private boolean configured = false;
	private final Map<Integer, List<Tuple2>> actionVariableUsageMap = new LinkedHashMap<>();

	public TransformationAction add(ActionVariable action) {
		checkConfigured();
		actionVariables.add(action);
		return this;
	}

	public TransformationAction add(InsertAction<?> action) {
		checkConfigured();
		insertActions.add(action);
		return this;
	}

	public TransformationAction add(DeleteAction action) {
		checkConfigured();
		deleteActions.add(action);
		return this;
	}

	private void checkConfigured() {
		if (configured) {
			throw new IllegalStateException("Action already configured.");
		}
	}

	public TransformationAction prepare(Model model) {
		for (ActionVariable action : actionVariables) {
			action.prepare(model);
		}
		for (InsertAction<?> action : insertActions) {
			action.prepare(model);
		}
		for (DeleteAction action : deleteActions) {
			action.prepare(model);
		}

		for (var insertAction : insertActions) {
			var actionIndex = insertActions.indexOf(insertAction);
			var variables = insertAction.getVariables();
			for (var i = 0; i < variables.length; i++) {
				var variablelGlobalIndex = actionVariables.indexOf(variables[i]);
				actionVariableUsageMap.computeIfAbsent(variablelGlobalIndex, k -> new ArrayList<>());
				actionVariableUsageMap.get(variablelGlobalIndex).add(Tuple.of(actionIndex, i));
			}
		}

		configured = true;
		return this;
	}

	public boolean fire(Tuple activation) {
		for (ActionVariable action : actionVariables) {
			action.fire(activation);
		}
		for (InsertAction<?> action : insertActions) {
			action.fire(activation);
		}
		for (DeleteAction action : deleteActions) {
			action.fire(activation);
		}
		return true;
	}

	// Returns true if ActionVariables and InsertActions are inserted in same order, ActionVariables are equal (they
	// have the same index for getting the value from the activation Tuple) and InsertActions are equal (they have
	// the same arity and value to be set).
	public boolean equalsWithSubstitution(TransformationAction other) {
		if (other == this) {
			return true;
		}

		if (actionVariables.size() != other.actionVariables.size()) {
			return false;
		}

		if (insertActions.size() != other.insertActions.size()) {
			return false;
		}

		if (deleteActions.size() != other.deleteActions.size()) {
			return false;
		}

		for (var i = 0; i < actionVariables.size(); i++) {
			var variable = actionVariables.get(i);
			var otherVariable = other.actionVariables.get(i);
			if (!variable.equalsWithSubstitution(otherVariable)) {
				return false;
			}
		}

		for (var i = 0; i < insertActions.size(); i++) {
			var insertAction = insertActions.get(i);
			var otherInsertAction = other.insertActions.get(i);
			if (!insertAction.equalsWithSubstitution(otherInsertAction)) {
				return false;
			}
		}

		for (var i = 0; i < deleteActions.size(); i++) {
			var deleteAction = deleteActions.get(i);
			var otherDeleteAction = other.deleteActions.get(i);
			if (!deleteAction.equalsWithSubstitution(otherDeleteAction)) {
				return false;
			}
		}
		return this.actionVariableUsageMap.equals(other.actionVariableUsageMap);

	}
}
