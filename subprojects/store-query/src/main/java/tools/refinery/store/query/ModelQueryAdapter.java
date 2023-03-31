package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.dnf.Query;

public interface ModelQueryAdapter extends ModelAdapter {
	ModelQueryStoreAdapter getStoreAdapter();

	default AnyResultSet getResultSet(AnyQuery query) {
		return getResultSet((Query<?>) query);
	}

	<T> ResultSet<T> getResultSet(Query<T> query);

	boolean hasPendingChanges();

	void flushChanges();
}
