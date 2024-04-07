/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.real;

import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.Term;

public abstract class RealUnaryTerm extends UnaryTerm<Double, Double> {
	protected RealUnaryTerm(Term<Double> body) {
		super(Double.class, Double.class, body);
	}
}
