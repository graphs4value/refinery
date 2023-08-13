/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.AbstractCountLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;

import java.util.List;

public class CountCandidateLowerBoundLiteral extends AbstractCountLiteral<Integer> {
	public CountCandidateLowerBoundLiteral(DataVariable<Integer> resultVariable, Constraint target,
										   List<Variable> arguments) {
		super(Integer.class, resultVariable, target, arguments);
	}

	@Override
	protected Integer zero() {
		return 0;
	}

	@Override
	protected Integer one() {
		return 1;
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CountCandidateLowerBoundLiteral(substitution.getTypeSafeSubstitute(getResultVariable()), getTarget(),
				substitutedArguments);
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new CountCandidateLowerBoundLiteral(getResultVariable(), newTarget, newArguments);
	}

	@Override
	protected String operatorName() {
		return "@LowerBound(\"candidate\") count";
	}
}
