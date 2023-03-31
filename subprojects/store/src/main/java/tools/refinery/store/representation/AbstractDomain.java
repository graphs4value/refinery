package tools.refinery.store.representation;

import java.util.Optional;

public non-sealed interface AbstractDomain<A, C> extends AnyAbstractDomain {
	@Override
	Class<A> abstractType();

	@Override
	Class<C> concreteType();

	A toAbstract(C concreteValue);

	Optional<C> toConcrete(A abstractValue);

	default boolean isConcrete(A abstractValue) {
		return toConcrete(abstractValue).isPresent();
	}

	boolean isRefinement(A originalValue, A refinedValue);

	A commonRefinement(A leftValue, A rightValue);

	A commonAncestor(A leftValue, A rightValue);

	A unknown();

	boolean isError(A abstractValue);
}
