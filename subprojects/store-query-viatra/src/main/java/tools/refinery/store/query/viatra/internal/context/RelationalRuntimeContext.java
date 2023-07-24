/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.context;

import org.eclipse.viatra.query.runtime.matchers.context.*;
import org.eclipse.viatra.query.runtime.matchers.tuple.ITuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import org.eclipse.viatra.query.runtime.matchers.util.Accuracy;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.viatra.internal.ViatraModelQueryAdapterImpl;
import tools.refinery.store.query.viatra.internal.pquery.SymbolViewWrapper;
import tools.refinery.store.query.viatra.internal.update.ModelUpdateListener;
import tools.refinery.store.query.view.AnySymbolView;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;

import static tools.refinery.store.util.CollectionsUtil.filter;
import static tools.refinery.store.util.CollectionsUtil.map;

public class RelationalRuntimeContext implements IQueryRuntimeContext {
	private final RelationalQueryMetaContext metaContext;

	private final ModelUpdateListener modelUpdateListener;

	private final Model model;

	RelationalRuntimeContext(ViatraModelQueryAdapterImpl adapter) {
		model = adapter.getModel();
		metaContext = new RelationalQueryMetaContext(adapter.getStoreAdapter().getInputKeys());
		modelUpdateListener = new ModelUpdateListener(adapter);
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
		if (key instanceof SymbolViewWrapper wrapper) {
			var symbolViewKey = wrapper.getWrappedKey();
			return this.modelUpdateListener.containsSymbolView(symbolViewKey);
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

	AnySymbolView checkKey(IInputKey key) {
		if (key instanceof SymbolViewWrapper wrappedKey) {
			var symbolViewKey = wrappedKey.getWrappedKey();
			if (modelUpdateListener.containsSymbolView(symbolViewKey)) {
				return symbolViewKey;
			} else {
				throw new IllegalStateException("Query is asking for non-indexed key %s".formatted(symbolViewKey));
			}
		} else {
			throw new IllegalStateException("Query is asking for non-relational key");
		}
	}

	@Override
	public int countTuples(IInputKey key, TupleMask seedMask, ITuple seed) {
		Iterator<Object[]> iterator = enumerate(key, seedMask, seed).iterator();
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
		var filteredBySeed = enumerate(key, seedMask, seed);
		return map(filteredBySeed, Tuples::flatTupleOf);
	}

	@Override
	public Iterable<?> enumerateValues(IInputKey key, TupleMask seedMask, ITuple seed) {
		var index = seedMask.getFirstOmittedIndex().orElseThrow(
				() -> new IllegalArgumentException("Seed mask does not omit a value"));
		var filteredBySeed = enumerate(key, seedMask, seed);
		return map(filteredBySeed, array -> array[index]);
	}

	private Iterable<Object[]> enumerate(IInputKey key, TupleMask seedMask, ITuple seed) {
		var relationViewKey = checkKey(key);
		Iterable<Object[]> allObjects = relationViewKey.getAll(model);
		return filter(allObjects, objectArray -> isMatching(objectArray, seedMask, seed));
	}

	private static boolean isMatching(Object[] tuple, TupleMask seedMask, ITuple seed) {
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
