/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;

import java.util.List;

public class CountLowerBoundLiteral extends ConcreteCountLiteral<Integer> {
	public CountLowerBoundLiteral(DataVariable<Integer> resultVariable, Concreteness concreteness, Constraint target,
								  List<Variable> arguments) {
		super(Integer.class, resultVariable, concreteness, target, arguments);
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
		return new CountLowerBoundLiteral(substitution.getTypeSafeSubstitute(getResultVariable()), getConcreteness(),
				getTarget(), substitutedArguments);
	}

	@Override
	protected AbstractCallLiteral internalWithTarget(Constraint newTarget) {
		return new CountLowerBoundLiteral(getResultVariable(), getConcreteness(), newTarget, getArguments());
	}

	@Override
	protected String operatorName() {
		return "@LowerBound(\"%s\") count".formatted(getConcreteness());
	}
}
