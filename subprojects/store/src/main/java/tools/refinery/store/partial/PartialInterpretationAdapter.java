package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelAdapter;

public interface PartialInterpretationAdapter extends ModelAdapter {
	@Override
	PartialInterpretationStoreAdapter getStoreAdapter();
}

