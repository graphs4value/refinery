/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.valuation.Valuation;

import java.util.Set;

public class NodeIdTerm extends AbstractTerm<Integer> {
	private final NodeVariable nodeVariable;

	public NodeIdTerm(NodeVariable nodeVariable) {
		super(Integer.class);
		this.nodeVariable = nodeVariable;
	}

	public NodeVariable getNodeVariable() {
		return nodeVariable;
	}

	@Override
	public Integer evaluate(Valuation valuation) {
		return valuation.getNodeId(nodeVariable);
	}

	@Override
	public Term<Integer> rewriteSubTerms(TermRewriter termRewriter) {
		return this;
	}

	@Override
	public Term<Integer> substitute(Substitution substitution) {
		return new NodeIdTerm(substitution.getTypeSafeSubstitute(nodeVariable));
	}

	@Override
	public Set<Variable> getVariables() {
		return Set.of(nodeVariable);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherNodeIdTerm = (NodeIdTerm) other;
		return helper.variableEqual(nodeVariable, otherNodeIdTerm.nodeVariable);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return 31 * super.hashCodeWithSubstitution(helper) + helper.getVariableHashCode(nodeVariable);
	}
}
