/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.exceptions.IncompatibleParameterDirectionException;
import tools.refinery.store.query.term.*;

import java.util.*;

public final class VariableBindingSiteBuilder {
	private final Map<Variable, VariableDirection> directionMap = new LinkedHashMap<>();
	private final Set<Variable> nonUnifiableVariables = new HashSet<>();

	VariableBindingSiteBuilder() {
	}

	public VariableBindingSiteBuilder variable(Variable variable, VariableDirection direction) {
		return variable(variable, direction, direction == VariableDirection.OUT);
	}

	public VariableBindingSiteBuilder variable(Variable variable, VariableDirection direction, boolean markAsUnique) {
		validateUnique(direction, markAsUnique);
		validateDirection(variable, direction);
		boolean unique;
		if (markAsUnique) {
			nonUnifiableVariables.add(variable);
			unique = true;
		} else {
			unique = nonUnifiableVariables.contains(variable);
		}
		directionMap.compute(variable, (ignored, oldValue) -> {
			if (oldValue == null) {
				return direction;
			}
			if (unique) {
				throw new IllegalArgumentException("Duplicate binding for variable " + variable);
			}
			try {
				return merge(oldValue, direction);
			} catch (IncompatibleParameterDirectionException e) {
				var message = "%s for variable %s".formatted(e.getMessage(), variable);
				throw new IncompatibleParameterDirectionException(message, e);
			}
		});
		return this;
	}

	private static void validateUnique(VariableDirection direction, boolean markAsUnique) {
		if (direction == VariableDirection.OUT && !markAsUnique) {
			throw new IllegalArgumentException("OUT binding must be marked as non-unifiable");
		}
		if (markAsUnique && (direction != VariableDirection.OUT && direction != VariableDirection.CLOSURE)) {
			throw new IllegalArgumentException("Only OUT or CLOSURE binding may be marked as non-unifiable");
		}
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

	private static VariableDirection merge(VariableDirection left, VariableDirection right) {
		return switch (left) {
			case IN_OUT -> switch (right) {
				case IN_OUT -> VariableDirection.IN_OUT;
				case OUT -> VariableDirection.OUT;
				case IN, NEUTRAL -> VariableDirection.IN;
				case CLOSURE -> throw incompatibleDirections(left, right);
			};
			case OUT -> switch (right) {
				case IN_OUT, OUT -> VariableDirection.OUT;
				case IN, NEUTRAL, CLOSURE -> throw incompatibleDirections(left, right);
			};
			case IN -> switch (right) {
				case IN_OUT, NEUTRAL, IN -> VariableDirection.IN;
				case OUT, CLOSURE -> throw incompatibleDirections(left, right);
			};
			case NEUTRAL -> switch (right) {
				case IN_OUT, IN -> VariableDirection.IN;
				case NEUTRAL -> VariableDirection.NEUTRAL;
				case CLOSURE -> VariableDirection.CLOSURE;
				case OUT -> throw incompatibleDirections(left, right);
			};
			case CLOSURE -> switch (right) {
				case NEUTRAL, CLOSURE -> VariableDirection.CLOSURE;
				case IN_OUT, IN, OUT -> throw incompatibleDirections(left, right);
			};
		};
	}

	private static RuntimeException incompatibleDirections(VariableDirection left, VariableDirection right) {
		return new IncompatibleParameterDirectionException("Incompatible variable directions %s and %s"
				.formatted(left, right));
	}

	public VariableBindingSiteBuilder variables(Collection<? extends Variable> variables, VariableDirection direction) {
		for (var variable : variables) {
			variable(variable, direction);
		}
		return this;
	}

	public VariableBindingSiteBuilder parameterList(boolean positive, List<Parameter> parameters,
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

	public VariableBindingSite build() {
		return new VariableBindingSite(directionMap);
	}
}
