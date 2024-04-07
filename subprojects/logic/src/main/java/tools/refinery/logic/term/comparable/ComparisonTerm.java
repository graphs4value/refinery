/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.comparable;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

public abstract class ComparisonTerm<T> extends BinaryTerm<Boolean, T, T> {
	protected ComparisonTerm(Class<T> argumentType, Term<T> left, Term<T> right) {
		super(Boolean.class, argumentType, argumentType, left, right);
	}

	public Class<T> getArgumentType() {
		return getLeftType();
	}
}
