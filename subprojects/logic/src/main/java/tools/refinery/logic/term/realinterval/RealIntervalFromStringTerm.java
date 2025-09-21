/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.string.StringValue;

import java.math.BigDecimal;

public class RealIntervalFromStringTerm extends UnaryTerm<RealInterval, StringValue> {
	protected RealIntervalFromStringTerm(Term<StringValue> body) {
		super(RealInterval.class, StringValue.class, body);
	}

	@Override
	protected RealInterval doEvaluate(StringValue bodyValue) {
		return switch (bodyValue) {
			case StringValue.Unknown ignored -> RealInterval.UNKNOWN;
			case StringValue.Error ignored -> RealInterval.ERROR;
			case StringValue.Concrete(var concreteValue) -> {
				BigDecimal value;
				try {
					value = new BigDecimal(concreteValue);
				} catch (NumberFormatException e) {
					yield RealInterval.ERROR;
				}
				yield RealInterval.of(value);
			}
		};
	}

	@Override
	protected Term<RealInterval> constructWithBody(Term<StringValue> newBody) {
		return new RealIntervalFromStringTerm(newBody);
	}
}
