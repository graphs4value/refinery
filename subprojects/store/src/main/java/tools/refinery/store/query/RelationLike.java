package tools.refinery.store.query;

public interface RelationLike {
	String name();

	int arity();

	default boolean invalidIndex(int i) {
		return i < 0 || i >= arity();
	}
}
