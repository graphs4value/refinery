package tools.refinery.store.query.viatra;

import tools.refinery.store.query.ModelQueryAdapter;

public interface ViatraModelQueryAdapter extends ModelQueryAdapter {
	@Override
	ViatraModelQueryStoreAdapter getStoreAdapter();
}
