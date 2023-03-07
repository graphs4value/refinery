package tools.refinery.store.query.dnf;

public sealed interface AnyQuery permits Query {
	String name();

	int arity();

	Class<?> valueType();

	Dnf getDnf();
}
