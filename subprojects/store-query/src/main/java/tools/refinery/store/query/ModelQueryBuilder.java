package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.AnyQuery;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public interface ModelQueryBuilder extends ModelAdapterBuilder {
	default ModelQueryBuilder queries(AnyQuery... queries) {
		return queries(List.of(queries));
	}

	default ModelQueryBuilder queries(Collection<? extends AnyQuery> queries) {
		queries.forEach(this::query);
		return this;
	}

	ModelQueryBuilder query(AnyQuery query);

	@Override
	ModelQueryStoreAdapter createStoreAdapter(ModelStore store);
}
