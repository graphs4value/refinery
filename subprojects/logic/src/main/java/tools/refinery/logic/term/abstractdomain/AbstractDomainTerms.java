package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.Term;

public final class AbstractDomainTerms {
	private AbstractDomainTerms() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static <A extends AbstractValue<A, C>, C> Term<Boolean> isError(AbstractDomain<A, C> abstractDomain,
																		   Term<A> body) {
		return new IsErrorTerm<>(abstractDomain, body);
	}
}
