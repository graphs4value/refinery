package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;

import java.util.Collection;
import java.util.List;

public interface ModelQueryBuilder extends ModelAdapterBuilder {
	default ModelQueryBuilder queries(DNF... queries) {
		return queries(List.of(queries));
	}

	default ModelQueryBuilder queries(Collection<? extends DNF> queries) {
		queries.forEach(this::query);
		return this;
	}

	ModelQueryBuilder query(DNF query);

	@Override
	ModelQueryStoreAdapter createStoreAdapter(ModelStore store);
}
