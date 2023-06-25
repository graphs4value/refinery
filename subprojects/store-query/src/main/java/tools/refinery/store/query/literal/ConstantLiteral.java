/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.Variable;

import java.util.Objects;
import java.util.Set;

public class ConstantLiteral extends AbstractLiteral {
	private final NodeVariable variable;
	private final int nodeId;

	public ConstantLiteral(NodeVariable variable, int nodeId) {
		this.variable = variable;
		this.nodeId = nodeId;
	}

	public NodeVariable getVariable() {
		return variable;
	}

	public int getNodeId() {
		return nodeId;
	}


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
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), helper.getVariableHashCode(variable), nodeId);
	}

	@Override
	public String toString() {
		return "%s === @Constant %d".formatted(variable, nodeId);
	}
}
