/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.Term;

public class AbstractDomainSupersetTerm<A extends AbstractValue<A, C>, C>
		extends AbstractDomainBinaryTerm<Boolean, A, C> {
	public AbstractDomainSupersetTerm(AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		super(Boolean.class, abstractDomain, left, right);
	}

	@Override
	protected Boolean doEvaluate(A leftValue, A rightValue) {
		return rightValue.isRefinementOf(leftValue);
	}

	@Override
	protected Term<Boolean> constructWithSubTerms(Term<A> newLeft, Term<A> newRight) {
		return new AbstractDomainSupersetTerm<>(getAbstractDomain(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s :> %s)".formatted(getLeft(), getRight());
	}
}
