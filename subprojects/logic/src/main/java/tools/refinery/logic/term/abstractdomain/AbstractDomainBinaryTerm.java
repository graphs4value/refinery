/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

import java.util.Objects;

public abstract class AbstractDomainBinaryTerm<R, A extends AbstractValue<A, C>, C> extends BinaryTerm<R, A, A> {
	private final AbstractDomain<A, C> abstractDomain;

	protected AbstractDomainBinaryTerm(Class<R> resultType, AbstractDomain<A, C> abstractDomain, Term<A> left,
									   Term<A> right) {
		super(resultType, abstractDomain.abstractType(), abstractDomain.abstractType(), left, right);
		this.abstractDomain = abstractDomain;
	}

	public AbstractDomain<A, C> getAbstractDomain() {
		return abstractDomain;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherAbstractDomainTerm = (AbstractDomainBinaryTerm<?, ?, ?>) other;
		return abstractDomain.equals(otherAbstractDomainTerm.abstractDomain);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), abstractDomain);
	}
}
