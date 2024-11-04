/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.int_;

import tools.refinery.logic.term.Term;

public class IntPowTerm extends IntBinaryTerm {
	public IntPowTerm(Term<Integer> left, Term<Integer> right) {
		super(left, right);
	}

	@Override
	public Term<Integer> withSubTerms(Term<Integer> newLeft,
									  Term<Integer> newRight) {
		return new IntPowTerm(newLeft, newRight);
	}

	@Override
	protected Integer doEvaluate(Integer leftValue, Integer rightValue) {
		return rightValue < 0 ? null : power(leftValue, rightValue);
	}

	private static int power(int base, int exponent) {
		int accum = 1;
		while (exponent > 0) {
			if (exponent % 2 == 1) {
				accum = accum * base;
			}
			base = base * base;
			exponent = exponent / 2;
		}
		return accum;
	}

	@Override
	public String toString() {
		return "(%s ** %s)".formatted(getLeft(), getRight());
	}
}
