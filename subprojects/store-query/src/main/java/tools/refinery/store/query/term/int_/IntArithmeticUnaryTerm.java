/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.ArithmeticUnaryOperator;
import tools.refinery.store.query.term.ArithmeticUnaryTerm;

public class IntArithmeticUnaryTerm extends ArithmeticUnaryTerm<Integer> {
	public IntArithmeticUnaryTerm(ArithmeticUnaryOperator operation, Term<Integer> body) {
		super(operation, body);
	}

	@Override
	public Class<Integer> getType() {
		return Integer.class;
	}

	@Override
	protected Term<Integer> doSubstitute(Substitution substitution, Term<Integer> substitutedBody) {
		return new IntArithmeticUnaryTerm(getOperator(), substitutedBody);
	}

	@Override
	protected Integer doEvaluate(Integer bodyValue) {
		return switch(getOperator()) {
			case PLUS -> bodyValue;
			case MINUS -> -bodyValue;
		};
	}
}
