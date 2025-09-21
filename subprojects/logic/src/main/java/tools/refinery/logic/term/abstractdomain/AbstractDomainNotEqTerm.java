/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

public class AbstractDomainNotEqTerm<A extends AbstractValue<A, C>, C>
		extends AbstractDomainBinaryTerm<TruthValue, A, C> {
	public AbstractDomainNotEqTerm(AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		super(TruthValue.class, abstractDomain, left, right);
	}

	@Override
	protected TruthValue doEvaluate(A leftValue, A rightValue) {
		return leftValue.checkEquals(rightValue).not();
	}

	@Override
	protected Term<TruthValue> constructWithSubTerms(Term<A> newLeft, Term<A> newRight) {
		return new AbstractDomainNotEqTerm<>(getAbstractDomain(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s != %s)".formatted(getLeft(), getRight());
	}
}
