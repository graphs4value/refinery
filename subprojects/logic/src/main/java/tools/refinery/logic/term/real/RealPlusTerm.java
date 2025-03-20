/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.real;

import tools.refinery.logic.term.Term;

public class RealPlusTerm extends RealUnaryTerm {
	public RealPlusTerm(Term<Double> body) {
		super(body);
	}

	@Override
	protected Term<Double> constructWithBody(Term<Double> newBody) {
		return new RealPlusTerm(newBody);
	}

	@Override
	protected Double doEvaluate(Double bodyValue) {
		return bodyValue;
	}

	@Override
	public String toString() {
		return "(+%s)".formatted(getBody());
	}
}
