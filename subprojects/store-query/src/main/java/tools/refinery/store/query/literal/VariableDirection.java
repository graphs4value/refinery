/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.term.ParameterDirection;

/**
 * Directions of the flow of a variable to ro from a clause.
 * <p>
 * During the evaluation of a clause
 * <ol>
 *     <li>reads already bound {@code IN} variables,</li>
 *     <li>enumerates over all possible bindings of {@code CLOSURE} variables, and</li>
 *     <li>
 *         produces bindings for
 *         <ul>
 *             <li>{@code IN_OUT} variables that may already be bound in clause (if they already have a binding,
 *             the existing values are compared to the new binding by {@link Object#equals(Object)}), and</li>
 *             <li>{@code OUT} variables that must not be already bound in the clause (because comparison by
 *             equality wouldn't produce an appropriate join).</li>
 *         </ul>
 *     </li>
 * </ol>
 * Variables marked as {@code NEUTRAL} may act as {@code IN} or {@code CLOSURE} depending on whether they have an
 * existing binding that can be read.
 */
public enum VariableDirection {
	/**
	 * Binds a node variable or check equality with a node variable.
	 * <p>
	 * This is the usual direction for positive constraints on nodes. A data variable may have multiple {@code IN_OUT}
	 * bindings, even on the same parameter list.
	 * <p>
	 * Cannot be used for data variables.
	 */
	IN_OUT,

	/**
	 * Binds a data variable.
	 * <p>
	 * A single variable must have at most one {@code OUT} binding. A variable with a {@code OUT} binding cannot
	 * appear in any other place in a parameter list.
	 * <p>
	 * Cannot be used for node variables.
	 */
	OUT,

	/**
	 * Takes an already bound variable.
	 * <p>
	 * May be used with node or data variables. An {@code IN_OUT} or {@code OUT} binding on the same parameter list
	 * cannot satisfy the {@code IN} binding, because it might introduce a (non-monotonic) circular dependency.
	 */
	IN,

	/**
	 * Either takes a bound data variable or enumerates all possible data variable bindings.
	 * <p>
	 * Cannot be used for data variables.
	 */
	NEUTRAL,

	/**
	 * Enumerates over all possible data variable bindings.
	 * <p>
	 * May be used with node or data variables. The variable may not appear in any other parameter list. A data
	 * variable may only appear once in the parameter list, but node variables can appear multiple times to form
	 * diagonal constraints.
	 */
	CLOSURE;

	public static VariableDirection from(boolean positive, ParameterDirection parameterDirection) {
		return switch (parameterDirection) {
			case IN_OUT -> positive ? IN_OUT : NEUTRAL;
			case OUT -> positive ? OUT : CLOSURE;
			case IN -> IN;
		};
	}
}
