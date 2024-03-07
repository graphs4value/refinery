/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.real;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

public abstract class RealBinaryTerm extends BinaryTerm<Double, Double, Double> {
	protected RealBinaryTerm(Term<Double> left, Term<Double> right) {
		super(Double.class, Double.class, Double.class, left, right);
	}
}
