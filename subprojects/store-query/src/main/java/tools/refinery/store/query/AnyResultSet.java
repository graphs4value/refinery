package tools.refinery.store.query;

import tools.refinery.store.query.dnf.AnyQuery;

public sealed interface AnyResultSet permits ResultSet {
	ModelQueryAdapter getAdapter();

	AnyQuery getQuery();

	int size();
}
