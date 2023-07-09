/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.dnf;

import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.Objects;

public final class SymbolicParameter extends Parameter {
	private final Variable variable;

	public SymbolicParameter(Variable variable, ParameterDirection direction) {
		super(variable.tryGetType().orElse(null), direction);
		this.variable = variable;
	}

	public Variable getVariable() {
		return variable;
	}

	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCode(), helper.getVariableHashCode(variable));
	}

	@Override
	public String toString() {
		var direction = getDirection();
		if (direction == ParameterDirection.OUT) {
			return variable.toString();
		}
		return "%s %s".formatted(getDirection(), variable);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SymbolicParameter that = (SymbolicParameter) o;
		return Objects.equals(variable, that.variable);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), variable);
	}
}
