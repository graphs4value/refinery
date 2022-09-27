package tools.refinery.store.query.viatra.internal.viewupdate;

import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.context.IQueryRuntimeContextListener;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.query.view.RelationView;

import java.util.Objects;

public class ViewUpdateTranslator<D> {
	private final IInputKey wrappedKey;

	private final RelationView<D> key;

	private final ITuple filter;

	private final IQueryRuntimeContextListener listener;

	public ViewUpdateTranslator(IInputKey wrappedKey, RelationView<D> key, ITuple filter,
								IQueryRuntimeContextListener listener) {
		super();
		this.wrappedKey = wrappedKey;
		this.key = key;
		this.filter = filter;
		this.listener = listener;
	}

	public boolean equals(IInputKey wrappedKey, RelationView<?> relationView, ITuple seed,
						  IQueryRuntimeContextListener listener) {
		return this.wrappedKey == wrappedKey && key == relationView && filter.equals(seed) && this.listener == listener;
	}

	public void processChange(ViewUpdate change) {
		listener.update(wrappedKey, Tuples.flatTupleOf(change.tuple()), change.isInsertion());
	}

	@SuppressWarnings("squid:S1168")
	public Object[] isMatching(Tuple tuple, D value) {
		if (!key.filter(tuple, value)) {
			return null;
		}
		return isMatching(key.forwardMap(tuple, value), filter);
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
