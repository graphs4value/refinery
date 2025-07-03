/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.ComparableAbstractDomain;
import tools.refinery.logic.term.ComparableAbstractValue;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

public class AbstractDomainGreaterEqTerm<A extends ComparableAbstractValue<A, C>, C extends Comparable<C>>
		extends AbstractDomainBinaryTerm<TruthValue, A, C> {
	public AbstractDomainGreaterEqTerm(ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		super(TruthValue.class, abstractDomain, left, right);
	}

	@Override
	protected TruthValue doEvaluate(A leftValue, A rightValue) {
		return rightValue.checkLessEq(leftValue);
	}

	@Override
	protected Term<TruthValue> constructWithSubTerms(Term<A> newLeft, Term<A> newRight) {
		return new AbstractDomainGreaterEqTerm<>((ComparableAbstractDomain<A, C>) getAbstractDomain(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s >= %s)".formatted(getLeft(), getRight());
	}
}
