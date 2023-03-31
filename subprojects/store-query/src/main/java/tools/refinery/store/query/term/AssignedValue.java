package tools.refinery.store.query.term;

import tools.refinery.store.query.literal.Literal;

@FunctionalInterface
public interface AssignedValue<T> {
	Literal toLiteral(DataVariable<T> targetVariable);
}
