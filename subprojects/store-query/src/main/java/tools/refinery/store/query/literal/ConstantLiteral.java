/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.NodeVariable;

import java.util.Objects;

public final class ConstantLiteral implements Literal {
	private final NodeVariable variable;
	private final int nodeId;
	private final VariableBinder variableBinder;

	public ConstantLiteral(NodeVariable variable, int nodeId) {
		this.variable = variable;
		this.nodeId = nodeId;
		variableBinder = VariableBinder.builder().variable(variable, VariableDirection.IN_OUT).build();
	}

	public NodeVariable getVariable() {
		return variable;
	}

	public int getNodeId() {
		return nodeId;
	}

	@Override
	public VariableBinder getVariableBinder() {
		return variableBinder;
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
