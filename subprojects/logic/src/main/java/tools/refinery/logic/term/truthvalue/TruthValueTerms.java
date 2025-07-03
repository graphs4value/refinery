/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.logic.term.operators.AndTerm;
import tools.refinery.logic.term.operators.NotTerm;
import tools.refinery.logic.term.operators.OrTerm;
import tools.refinery.logic.term.operators.XorTerm;

public class TruthValueTerms {
	private TruthValueTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static Term<TruthValue> constant(TruthValue value) {
		return new ConstantTerm<>(TruthValue.class, value);
	}

	public static Term<Boolean> may(Term<TruthValue> body) {
		return new TruthValueMayTerm(body);
	}

	public static Term<Boolean> must(Term<TruthValue> body) {
		return new TruthValueMustTerm(body);
	}

	public static Term<TruthValue> asTruthValue(Term<Boolean> body) {
		return new AsTruthValueTerm(body);
	}

	public static Term<TruthValue> not(Term<TruthValue> body) {
		return new NotTerm<>(TruthValue.class, body);
	}

	public static Term<TruthValue> and(Term<TruthValue> left, Term<TruthValue> right) {
		return new AndTerm<>(TruthValue.class, left, right);
	}

	public static Term<TruthValue> or(Term<TruthValue> left, Term<TruthValue> right) {
		return new OrTerm<>(TruthValue.class, left, right);
	}

	public static Term<TruthValue> xor(Term<TruthValue> left, Term<TruthValue> right) {
		return new XorTerm<>(TruthValue.class, left, right);
	}

	public static Term<TruthValue> eq(Term<TruthValue> left, Term<TruthValue> right) {
		return AbstractDomainTerms.eq(TruthValueDomain.INSTANCE, left, right);
	}

	public static Term<TruthValue> notEq(Term<TruthValue> left, Term<TruthValue> right) {
		return AbstractDomainTerms.notEq(TruthValueDomain.INSTANCE, left, right);
	}
}
