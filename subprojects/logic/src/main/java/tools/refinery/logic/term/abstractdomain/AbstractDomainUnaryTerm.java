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
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;

import java.util.Objects;

public abstract class AbstractDomainUnaryTerm<R, A extends AbstractValue<A, C>, C> extends UnaryTerm<R, A> {
	private final AbstractDomain<A, C> abstractDomain;

	protected AbstractDomainUnaryTerm(Class<R> type, AbstractDomain<A, C> abstractDomain, Term<A> body) {
		super(type, abstractDomain.abstractType(), body);
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
		var otherUnaryTerm = (AbstractDomainUnaryTerm<?, ?, ?>) other;
		return abstractDomain.equals(otherUnaryTerm.abstractDomain);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), abstractDomain);
	}
}
