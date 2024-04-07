/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.AbstractCountLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;

import java.util.List;

public class CountUpperBoundLiteral extends AbstractCountLiteral<UpperCardinality> {
	public CountUpperBoundLiteral(DataVariable<UpperCardinality> resultVariable, Constraint target,
								  List<Variable> arguments) {
		super(UpperCardinality.class, resultVariable, target, arguments);
	}

	@Override
	protected UpperCardinality zero() {
		return UpperCardinalities.ZERO;
	}

	@Override
	protected UpperCardinality one() {
		return UpperCardinalities.UNBOUNDED;
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CountUpperBoundLiteral(substitution.getTypeSafeSubstitute(getResultVariable()), getTarget(),
				substitutedArguments);
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new CountUpperBoundLiteral(getResultVariable(), newTarget, newArguments);
	}

	@Override
	protected String operatorName() {
		return "@UpperBound(\"partial\") count";
	}
}
