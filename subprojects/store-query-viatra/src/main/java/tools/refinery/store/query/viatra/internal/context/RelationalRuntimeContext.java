package tools.refinery.store.query.viatra.internal.context;

import org.eclipse.viatra.query.runtime.matchers.context.*;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.eclipse.viatra.query.runtime.matchers.util.Accuracy;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.viatra.internal.pquery.RelationViewWrapper;
import tools.refinery.store.query.viatra.internal.update.ModelUpdateListener;
import tools.refinery.store.query.view.AnyRelationView;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;

import static tools.refinery.store.util.CollectionsUtil.filter;
import static tools.refinery.store.util.CollectionsUtil.map;

public class RelationalRuntimeContext implements IQueryRuntimeContext {
	private final RelationalQueryMetaContext metaContext = new RelationalQueryMetaContext();

	private final ModelUpdateListener modelUpdateListener;

	private final Model model;

	public RelationalRuntimeContext(Model model, ModelUpdateListener relationUpdateListener) {
		this.model = model;
		this.modelUpdateListener = relationUpdateListener;
	}

	@Override
	public IQueryMetaContext getMetaContext() {
		return metaContext;
	}

	@Override
	public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException {
		try {
			return callable.call();
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

	@Override
	public boolean isCoalescing() {
		return false;
	}

	@Override
	public boolean isIndexed(IInputKey key, IndexingService service) {
		if (key instanceof AnyRelationView relationalKey) {
			return this.modelUpdateListener.containsRelationView(relationalKey);
		} else {
			return false;
		}
	}

	@Override
	public void ensureIndexed(IInputKey key, IndexingService service) {
		if (!isIndexed(key, service)) {
			throw new IllegalStateException("Engine tries to index a new key %s".formatted(key));
		}
	}

	AnyRelationView checkKey(IInputKey key) {
		if (key instanceof RelationViewWrapper wrappedKey) {
			var relationViewKey = wrappedKey.getWrappedKey();
			if (modelUpdateListener.containsRelationView(relationViewKey)) {
				return relationViewKey;
			} else {
				throw new IllegalStateException("Query is asking for non-indexed key %s".formatted(relationViewKey));
			}
		} else {
			throw new IllegalStateException("Query is asking for non-relational key");
		}
	}

	@Override
	public int countTuples(IInputKey key, TupleMask seedMask, ITuple seed) {
		var relationViewKey = checkKey(key);
		Iterable<Object[]> allObjects = relationViewKey.getAll(model);
		Iterable<Object[]> filteredBySeed = filter(allObjects, objectArray -> isMatching(objectArray, seedMask, seed));
		Iterator<Object[]> iterator = filteredBySeed.iterator();
		int result = 0;
		while (iterator.hasNext()) {
			iterator.next();
			result++;
		}
		return result;
	}

	@Override
	public Optional<Long> estimateCardinality(IInputKey key, TupleMask groupMask, Accuracy requiredAccuracy) {
		return Optional.empty();
	}

	@Override
	public Iterable<Tuple> enumerateTuples(IInputKey key, TupleMask seedMask, ITuple seed) {
		var relationViewKey = checkKey(key);
		Iterable<Object[]> allObjects = relationViewKey.getAll(model);
		Iterable<Object[]> filteredBySeed = filter(allObjects, objectArray -> isMatching(objectArray, seedMask, seed));
		return map(filteredBySeed, Tuples::flatTupleOf);
	}

	private boolean isMatching(Object[] tuple, TupleMask seedMask, ITuple seed) {
		for (int i = 0; i < seedMask.indices.length; i++) {
			final Object seedElement = seed.get(i);
			final Object tupleElement = tuple[seedMask.indices[i]];
			if (!tupleElement.equals(seedElement)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Iterable<?> enumerateValues(IInputKey key, TupleMask seedMask, ITuple seed) {
		return enumerateTuples(key, seedMask, seed);
	}

	@Override
	public boolean containsTuple(IInputKey key, ITuple seed) {
		var relationViewKey = checkKey(key);
		return relationViewKey.get(model, seed.getElements());
	}

	@Override
	public void addUpdateListener(IInputKey key, Tuple seed, IQueryRuntimeContextListener listener) {
		var relationViewKey = checkKey(key);
		this.modelUpdateListener.addListener(key, relationViewKey, seed, listener);

	}

	@Override
	public void removeUpdateListener(IInputKey key, Tuple seed, IQueryRuntimeContextListener listener) {
		var relationViewKey = checkKey(key);
		this.modelUpdateListener.removeListener(key, relationViewKey, seed, listener);
	}

	@Override
	public Object wrapElement(Object externalElement) {
		return externalElement;
	}

	@Override
	public Object unwrapElement(Object internalElement) {
		return internalElement;
	}

	@Override
	public Tuple wrapTuple(Tuple externalElements) {
		return externalElements;
	}

	@Override
	public Tuple unwrapTuple(Tuple internalElements) {
		return internalElements;
	}

	@Override
	public void ensureWildcardIndexing(IndexingService service) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void executeAfterTraversal(Runnable runnable) {
		runnable.run();
	}
}
