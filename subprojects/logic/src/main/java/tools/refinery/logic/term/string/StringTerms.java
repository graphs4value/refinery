/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.string;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.operators.AddTerm;

public class StringTerms {
	private StringTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<StringValue> constant(StringValue value) {
		return new ConstantTerm<>(StringValue.class, value);
	}

	public static Term<StringValue> add(Term<StringValue> left, Term<StringValue> right) {
		return new AddTerm<>(StringValue.class, left, right);
	}
}
