package tools.refinery.store.query.viatra.internal.update;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.query.view.RelationView;
import tools.refinery.store.tuple.Tuple;

import java.util.Arrays;

public class TupleChangingRelationViewUpdateListener<T> extends RelationViewUpdateListener<T> {
	private final RelationView<T> relationView;

	TupleChangingRelationViewUpdateListener(ViatraModelQueryAdapterImpl adapter, RelationView<T> relationView,
											Interpretation<T> interpretation) {
        super(adapter, interpretation);
        this.relationView = relationView;
	}

	@Override
	public void put(Tuple key, T fromValue, T toValue, boolean restoring) {
		boolean fromPresent = relationView.filter(key, fromValue);
		boolean toPresent = relationView.filter(key, toValue);
		if (fromPresent) {
			if (toPresent) { // value change
				var fromArray = relationView.forwardMap(key, fromValue);
				var toArray = relationView.forwardMap(key, toValue);
				if (!Arrays.equals(fromArray, toArray)) {
					processUpdate(Tuples.flatTupleOf(fromArray), false);
					processUpdate(Tuples.flatTupleOf(toArray), true);
				}
			} else { // fromValue disappears
				processUpdate(Tuples.flatTupleOf(relationView.forwardMap(key, fromValue)), false);
			}
		} else if (toPresent) { // toValue appears
			processUpdate(Tuples.flatTupleOf(relationView.forwardMap(key, toValue)), true);
		}
	}
}
