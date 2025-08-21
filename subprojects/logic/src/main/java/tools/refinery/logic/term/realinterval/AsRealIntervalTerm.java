/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.intinterval.IntInterval;

public class AsRealIntervalTerm extends UnaryTerm<RealInterval, IntInterval> {
	AsRealIntervalTerm(Term<IntInterval> body) {
		super(RealInterval.class, IntInterval.class, body);
	}

	@Override
	protected RealInterval doEvaluate(IntInterval bodyValue) {
		return RealInterval.fromInteger(bodyValue);
	}

	@Override
	protected Term<RealInterval> constructWithBody(Term<IntInterval> newBody) {
		return new AsRealIntervalTerm(newBody);
	}
}
