/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.exceptions.IncompatibleParameterDirectionException;
import tools.refinery.store.query.term.*;

import java.util.*;

public final class VariableBinderBuilder {
	private final Map<Variable, VariableDirection> directionMap = new LinkedHashMap<>();
	private final Set<Variable> uniqueVariables = new HashSet<>();

	VariableBinderBuilder() {
	}

	public VariableBinderBuilder variable(Variable variable, VariableDirection direction) {
		return variable(variable, direction, direction == VariableDirection.OUT);
	}

	private VariableBinderBuilder variable(Variable variable, VariableDirection direction, boolean markAsUnique) {
		validateDirection(variable, direction);
		boolean unique = shouldBeUnique(variable, markAsUnique);
		directionMap.compute(variable, (ignored, oldValue) -> {
			if (oldValue == null) {
				return direction;
			}
			if (unique) {
				throw new IllegalArgumentException("Duplicate binding for variable " + variable);
			}
			try {
				return oldValue.merge(direction);
			} catch (IncompatibleParameterDirectionException e) {
				var message = "%s for variable %s".formatted(e.getMessage(), variable);
				throw new IncompatibleParameterDirectionException(message, e);
			}
		});
		return this;
	}

	private static void validateDirection(Variable variable, VariableDirection direction) {
		if (variable instanceof AnyDataVariable) {
			if (direction == VariableDirection.IN_OUT) {
				throw new IllegalArgumentException("%s direction is not supported for data variable %s"
						.formatted(direction, variable));
			}
		} else if (variable instanceof NodeVariable) {
			if (direction == VariableDirection.OUT) {
				throw new IllegalArgumentException("%s direction is not supported for node variable %s"
						.formatted(direction, variable));
			}
		} else {
			throw new IllegalArgumentException("Unknown variable " + variable);
		}
	}

	private boolean shouldBeUnique(Variable variable, boolean markAsUnique) {
		if (markAsUnique) {
			uniqueVariables.add(variable);
			return true;
		} else {
			return uniqueVariables.contains(variable);
		}
	}

	public VariableBinderBuilder variables(Collection<? extends Variable> variables, VariableDirection direction) {
		for (var variable : variables) {
			variable(variable, direction);
		}
		return this;
	}

	public VariableBinderBuilder parameterList(boolean positive, List<Parameter> parameters,
											   List<Variable> arguments) {
		int arity = parameters.size();
		if (arity != arguments.size()) {
			throw new IllegalArgumentException("Got %d arguments for %d parameters"
					.formatted(arguments.size(), arity));
		}
		for (int i = 0; i < arity; i++) {
			var argument = arguments.get(i);
			var parameter = parameters.get(i);
			var parameterDirection = parameter.getDirection();
			var direction = VariableDirection.from(positive, parameterDirection);
			variable(argument, direction, parameterDirection == ParameterDirection.OUT);
		}
		return this;
	}

	public VariableBinder build() {
		return new VariableBinder(directionMap);
	}
}
