/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.exceptions.IncompatibleParameterDirectionException;
import tools.refinery.store.query.term.ParameterDirection;

public enum VariableDirection {
	/**
	 * Binds a node variable or check equality with a node variable.
	 * <p>
	 * This is the usual direction for positive constraints on nodes. A data variable may have multiple {@code InOut}
	 * bindings, even on the same parameter list.
	 * <p>
	 * Cannot be used for data variables.
	 */
	IN_OUT("@InOut"),

	/**
	 * Binds a data variable.
	 * <p>
	 * A single variable must have at most one {@code @Out} binding. A variable with a {@code @Out} binding cannot
	 * appear in any other place in a parameter list.
	 * <p>
	 * Cannot be used for node variables.
	 */
	OUT("@Out"),

	/**
	 * Either takes a bound data variable or enumerates all possible data variable bindings.
	 * <p>
	 * Cannot be used for data variables.
	 */
	NEGATIVE("@Negative"),

	/**
	 * Takes an already bound variable.
	 * <p>
	 * May be used with node or data variables. An {@code @InOut} or {@code @Out} binding on the same parameter list
	 * cannot satisfy the {@code @In} binding, because it might introduce a (non-monotonic) circular dependency.
	 */
	IN("@In"),

	/**
	 * Enumerates over all possible data variable bindings.
	 * <p>
	 * May be used with node or data variables. The variable may not appear in any other parameter list. A data
	 * variable may only appear once in the parameter list, but node variables can appear multiple times to form
	 * diagonal constraints.
	 */
	CLOSURE("@Closure");

	private final String name;

	VariableDirection(String name) {
		this.name = name;
	}

	public VariableDirection merge(VariableDirection other) {
		switch (this) {
		case IN_OUT -> {
			if (other == IN_OUT) {
				return this;
			}
		}
		case OUT -> {
			if (other == OUT) {
				throw new IncompatibleParameterDirectionException("Multiple %s bindings".formatted(this));
			}
		}
		case NEGATIVE -> {
			if (other == NEGATIVE || other == IN || other == CLOSURE) {
				return other;
			}
		}
		case IN, CLOSURE -> {
			if (other == NEGATIVE || other == this) {
				return this;
			}
		}
		}
		throw new IncompatibleParameterDirectionException("Incompatible variable directions %s and %s"
				.formatted(this, other));
	}

	@Override
	public String toString() {
		return name;
	}

	public static VariableDirection from(boolean positive, ParameterDirection parameterDirection) {
		return switch (parameterDirection) {
			case IN_OUT -> positive ? IN_OUT : NEGATIVE;
			case OUT -> positive ? OUT : CLOSURE;
			case IN -> IN;
		};
	}
}
