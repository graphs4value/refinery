/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.bool;

import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.Term;

public final class BoolTerms {
	private BoolTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<Boolean> constant(Boolean value) {
		return new ConstantTerm<>(Boolean.class, value);
	}

	public static Term<Boolean> not(Term<Boolean> body) {
		return new BoolNotTerm(body);
	}

	public static Term<Boolean> and(Term<Boolean> left, Term<Boolean> right) {
		return new BoolAndTerm(left, right);
	}

	public static Term<Boolean> or(Term<Boolean> left, Term<Boolean> right) {
		return new BoolOrTerm(left, right);
	}

	public static Term<Boolean> xor(Term<Boolean> left, Term<Boolean> right) {
		return new BoolXorTerm(left, right);
	}
}
