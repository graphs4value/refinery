/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.Objects;
import java.util.Set;

public record ConstantLiteral(NodeVariable variable, int nodeId) implements Literal {
	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of(variable);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of();
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of();
	}

	@Override
	public ConstantLiteral substitute(Substitution substitution) {
		return new ConstantLiteral(substitution.getTypeSafeSubstitute(variable), nodeId);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other.getClass() != getClass()) {
			return false;
		}
		var otherConstantLiteral = (ConstantLiteral) other;
		return helper.variableEqual(variable, otherConstantLiteral.variable) && nodeId == otherConstantLiteral.nodeId;
	}


	@Override
	public String toString() {
		return "%s === @Constant %d".formatted(variable, nodeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (ConstantLiteral) obj;
		return Objects.equals(this.variable, that.variable) &&
				this.nodeId == that.nodeId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), variable, nodeId);
	}
}
