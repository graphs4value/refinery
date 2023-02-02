package tools.refinery.store.query.viatra.internal.update;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContextListener;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import tools.refinery.store.model.InterpretationListener;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.query.view.RelationView;
import tools.refinery.store.query.view.TuplePreservingRelationView;

import java.util.ArrayList;
import java.util.List;

public abstract class RelationViewUpdateListener<T> implements InterpretationListener<T> {
	private final ViatraModelQueryAdapterImpl adapter;
	private final List<RelationViewFilter> filters = new ArrayList<>();

	protected RelationViewUpdateListener(ViatraModelQueryAdapterImpl adapter) {
		this.adapter = adapter;
	}

	public void addFilter(IInputKey inputKey, ITuple seed, IQueryRuntimeContextListener listener) {
		filters.add(new RelationViewFilter(inputKey, seed, listener));
	}

	public void removeFilter(IInputKey inputKey, ITuple seed, IQueryRuntimeContextListener listener) {
		filters.remove(new RelationViewFilter(inputKey, seed, listener));
	}

	protected void processUpdate(Tuple tuple, boolean isInsertion) {
		adapter.markAsPending();
		int size = filters.size();
		// Use a for loop instead of a for-each loop to avoid <code>Iterator</code> allocation overhead.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < size; i++) {
			filters.get(i).update(tuple, isInsertion);
		}
	}

	public static <T> RelationViewUpdateListener<T> of(ViatraModelQueryAdapterImpl adapter,
													   RelationView<T> relationView) {
		if (relationView instanceof TuplePreservingRelationView<T> tuplePreservingRelationView) {
			return new TuplePreservingRelationViewUpdateListener<>(adapter, tuplePreservingRelationView);
		}
		return new TupleChangingRelationViewUpdateListener<>(adapter, relationView);
	}
}
