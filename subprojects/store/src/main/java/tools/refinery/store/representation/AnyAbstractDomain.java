package tools.refinery.store.representation;

public sealed interface AnyAbstractDomain permits AbstractDomain {
	Class<?> abstractType();

	Class<?> concreteType();
}
