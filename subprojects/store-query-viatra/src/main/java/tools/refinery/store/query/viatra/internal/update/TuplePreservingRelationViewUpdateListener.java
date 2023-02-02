package tools.refinery.store.query.viatra.internal.update;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.query.view.TuplePreservingRelationView;
import tools.refinery.store.tuple.Tuple;

public class TuplePreservingRelationViewUpdateListener<T> extends RelationViewUpdateListener<T> {
	private final TuplePreservingRelationView<T> view;

	TuplePreservingRelationViewUpdateListener(ViatraModelQueryAdapterImpl adapter,
											  TuplePreservingRelationView<T> view) {
        super(adapter);
        this.view = view;
	}

	@Override
	public void put(Tuple key, T fromValue, T toValue, boolean restoring) {
		boolean fromPresent = view.filter(key, fromValue);
		boolean toPresent = view.filter(key, toValue);
		if (fromPresent == toPresent) {
			return;
		}
		var translated = Tuples.flatTupleOf(view.forwardMap(key));
		processUpdate(translated, toPresent);
	}
}
