package tools.refinery.store.query.dnf;

public sealed interface Query<T> extends AnyQuery permits RelationalQuery, FunctionalQuery {
	@Override
	Class<T> valueType();

	T defaultValue();

	static QueryBuilder builder() {
		return new QueryBuilder();
	}

	static QueryBuilder builder(String name) {
		return new QueryBuilder(name);
	}
}
