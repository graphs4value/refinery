package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple2;

import java.util.*;

public class TransformationAction {
	private final List<ActionSymbol> actionSymbols = new ArrayList<>();
	private final List<InsertAction<?>> insertActions = new ArrayList<>();
	private boolean configured = false;
	private final Map<Integer, List<Tuple2>> activationSymbolUsageMap = new LinkedHashMap<>();

	public TransformationAction add(ActionSymbol action) {
		checkConfigured();
		actionSymbols.add(action);
		return this;
	}

	public TransformationAction add(InsertAction<?> action) {
		checkConfigured();
		insertActions.add(action);
		return this;
	}

	private void checkConfigured() {
		if (configured) {
			throw new IllegalStateException("Action already configured.");
		}
	}

	public TransformationAction prepare(Model model) {
		for (ActionSymbol action : actionSymbols) {
			action.prepare(model);
		}
		for (InsertAction<?> action : insertActions) {
			action.prepare(model);
		}

		for (var insertAction : insertActions) {
			var actionIndex = insertActions.indexOf(insertAction);
			var symbols = insertAction.getSymbols();
			for (var i = 0; i < symbols.length; i++) {
				var symbolGlobalIndex = actionSymbols.indexOf(symbols[i]);
				activationSymbolUsageMap.computeIfAbsent(symbolGlobalIndex, k -> new ArrayList<>());
				activationSymbolUsageMap.get(symbolGlobalIndex).add(Tuple.of(actionIndex, i));
			}
		}

		configured = true;
		return this;
	}

	public boolean fire(Tuple activation) {
		for (ActionSymbol action : actionSymbols) {
			action.fire(activation);
		}
		for (InsertAction<?> action : insertActions) {
			action.fire(activation);
		}
		return true;
	}

	// True if Symbols and InsertActions are inserted in same order, ActivationSymbols are equal (they have the same
	// index for getting the value from the activation Tuple) and InsertActions are equal (they have the same arity
	// and value to be set).
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TransformationAction other)) {
			return false;
		}
		if (!actionSymbols.equals(other.actionSymbols)) {
			return false;
		}
		if (!insertActions.equals(other.insertActions)) {
			return false;
		}
		return this.activationSymbolUsageMap.equals(other.activationSymbolUsageMap);

	}

	@Override
	public int hashCode() {
		var result = 17;
		result = 31 * result + actionSymbols.hashCode();
		result = 31 * result + insertActions.hashCode();
		return result;
	}
}
