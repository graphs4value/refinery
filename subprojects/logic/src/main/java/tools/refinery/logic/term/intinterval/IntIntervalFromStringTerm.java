/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.string.StringValue;

public class IntIntervalFromStringTerm extends UnaryTerm<IntInterval, StringValue> {
	protected IntIntervalFromStringTerm(Term<StringValue> body) {
		super(IntInterval.class, StringValue.class, body);
	}

	@Override
	protected IntInterval doEvaluate(StringValue bodyValue) {
		return switch (bodyValue) {
			case StringValue.Unknown ignored -> IntInterval.UNKNOWN;
			case StringValue.Error ignored -> IntInterval.ERROR;
			case StringValue.Concrete(var concreteValue) -> {
				int value;
				try {
					value = Integer.parseInt(concreteValue, 10);
				} catch (NumberFormatException e) {
					yield IntInterval.ERROR;
				}
				yield IntInterval.of(value);
			}
		};
	}

	@Override
	protected Term<IntInterval> constructWithBody(Term<StringValue> newBody) {
		return new IntIntervalFromStringTerm(newBody);
	}
}
