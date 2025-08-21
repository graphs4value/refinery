/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.intinterval.IntInterval;

public class AsIntIntervalTerm extends UnaryTerm<IntInterval, RealInterval> {
	AsIntIntervalTerm(Term<RealInterval> body) {
		super(IntInterval.class, RealInterval.class, body);
	}

	@Override
	protected IntInterval doEvaluate(RealInterval bodyValue) {
		return bodyValue.asInt();
	}

	@Override
	protected Term<IntInterval> constructWithBody(Term<RealInterval> newBody) {
		return null;
	}
}
