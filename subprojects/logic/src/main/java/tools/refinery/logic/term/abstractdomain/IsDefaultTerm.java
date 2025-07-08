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

public class IsDefaultTerm<A extends AbstractValue<A, C>, C> extends UnaryTerm<Boolean, A> {
	private final AbstractDomain<A, C> abstractDomain;

	protected IsDefaultTerm(AbstractDomain<A, C> abstractDomain, Term<A> body) {
		super(Boolean.class, abstractDomain.abstractType(), body);
		this.abstractDomain = abstractDomain;
	}

	@Override
	protected Boolean doEvaluate(A bodyValue) {
		return abstractDomain.error().equals(bodyValue);
	}

	@Override
	protected Term<Boolean> constructWithBody(Term<A> newBody) {
		return new IsDefaultTerm<>(abstractDomain, newBody);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherIsErrorTerm = (IsDefaultTerm<?, ?>) other;
		return abstractDomain.equals(otherIsErrorTerm.abstractDomain);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), abstractDomain);
	}

	@Override
	public String toString() {
		return "isDefault(%s)".formatted(super.toString());
	}
}
