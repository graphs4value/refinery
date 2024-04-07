/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.real;

import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.Term;

public class RealPowTerm extends RealBinaryTerm {
	public RealPowTerm(Term<Double> left, Term<Double> right) {
		super(left, right);
	}

	@Override
	public Term<Double> doSubstitute(Substitution substitution, Term<Double> substitutedLeft,
									 Term<Double> substitutedRight) {
		return new RealPowTerm(substitutedLeft, substitutedRight);
	}

	@Override
	protected Double doEvaluate(Double leftValue, Double rightValue) {
		return Math.pow(leftValue, rightValue);
	}

	@Override
	public String toString() {
		return "(%s ** %s)".formatted(getLeft(), getRight());
	}
}
