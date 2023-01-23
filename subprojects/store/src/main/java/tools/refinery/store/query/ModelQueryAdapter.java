package tools.refinery.store.query;

import tools.refinery.store.adapter.ModelAdapter;

public interface ModelQueryAdapter extends ModelAdapter {
	ModelQueryStoreAdapter getStoreAdapter();

	ResultSet getResultSet(DNF query);

	void flushChanges();
}
