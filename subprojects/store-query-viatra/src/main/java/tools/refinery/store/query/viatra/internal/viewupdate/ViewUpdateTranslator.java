package tools.refinery.store.query.viatra.internal.viewupdate;

import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContextListener;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.query.view.RelationView;

import java.util.Objects;

public class ViewUpdateTranslator<D> {
	private final RelationView<D> key;

	private final ITuple filter;

	private final IQueryRuntimeContextListener listener;

	public ViewUpdateTranslator(RelationView<D> key, ITuple filter, IQueryRuntimeContextListener listener) {
		super();
		this.key = key;
		this.filter = filter;
		this.listener = listener;
	}

	public boolean equals(RelationView<?> relationView, ITuple seed, IQueryRuntimeContextListener listener) {
		return key == relationView && filter.equals(seed) && this.listener == listener;
	}

	public void processChange(ViewUpdate change) {
		listener.update(key, Tuples.flatTupleOf(change.tuple()), change.isInsertion());
	}

	public Object[] isMatching(Tuple tuple, D value) {
		return isMatching(key.getWrappedKey().transform(tuple, value), filter);
	}

	@SuppressWarnings("squid:S1168")
	private Object[] isMatching(Object[] tuple, ITuple filter) {
		for (int i = 0; i < filter.getSize(); i++) {
			final Object filterObject = filter.get(i);
			if (filterObject != null && !filterObject.equals(tuple[i])) {
				return null;
			}
		}
		return tuple;
	}

	@Override
	public int hashCode() {
		return Objects.hash(filter, key, listener);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ViewUpdateTranslator<?> other))
			return false;
		return Objects.equals(filter, other.filter) && Objects.equals(key, other.key)
				&& Objects.equals(listener, other.listener);
	}
}
