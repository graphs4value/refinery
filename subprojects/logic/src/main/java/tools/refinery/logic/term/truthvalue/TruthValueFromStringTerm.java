/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.string.StringValue;

public class TruthValueFromStringTerm extends UnaryTerm<TruthValue, StringValue> {
	TruthValueFromStringTerm(Term<StringValue> body) {
		super(TruthValue.class, StringValue.class, body);
	}

	@Override
	protected TruthValue doEvaluate(StringValue bodyValue) {
		return switch (bodyValue) {
			case StringValue.Unknown ignored -> TruthValue.UNKNOWN;
			case StringValue.Error ignored -> TruthValue.ERROR;
			case StringValue.Concrete(var concreteValue) -> {
				if (TruthValue.TRUE.getName().equals(concreteValue)) {
					yield TruthValue.TRUE;
				}
				if (TruthValue.FALSE.getName().equals(concreteValue)) {
					yield TruthValue.FALSE;
				}
				yield TruthValue.ERROR;
			}
		};
	}

	@Override
	protected Term<TruthValue> constructWithBody(Term<StringValue> newBody) {
		return new TruthValueFromStringTerm(newBody);
	}
}
