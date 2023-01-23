package tools.refinery.store.model.internal;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.internal.MapDiffCursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.InterpretationListener;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.TupleHashProvider;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;

public class VersionedInterpretation<T> implements Interpretation<T> {
	private final ModelImpl model;
	private final Symbol<T> symbol;
	private final VersionedMapStore<Tuple, T> store;
	private final VersionedMap<Tuple, T> map;
	private final List<InterpretationListener<T>> listeners = new ArrayList<>();
	private final List<InterpretationListener<T>> restoreListeners = new ArrayList<>();

	private VersionedInterpretation(ModelImpl model, Symbol<T> symbol, VersionedMapStore<Tuple, T> store,
									VersionedMap<Tuple, T> map) {
		this.model = model;
		this.symbol = symbol;
		this.store = store;
		this.map = map;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public Symbol<T> getSymbol() {
		return symbol;
	}

	@Override
	public long getSize() {
		return map.getSize();
	}

	private void checkKey(Tuple key) {
		if (key == null || key.getSize() != symbol.arity()) {
			throw new IllegalArgumentException("Key for %s must be a tuple with arity %s"
					.formatted(symbol, symbol.arity()));
		}
	}
	@Override
	public T get(Tuple key) {
		checkKey(key);
		return map.get(key);
	}

	@Override
	public Cursor<Tuple, T> getAll() {
		return map.getAll();
	}

	private void notifyListeners(Tuple key, T fromValue, T toValue, boolean restoring) {
		var listenerList = restoring ? restoreListeners : listeners;
		int listenerCount = listenerList.size();
		// Use a for loop instead of a for-each loop to avoid <code>Iterator</code> allocation overhead.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < listenerCount; i++) {
			listenerList.get(i).put(key, fromValue, toValue, restoring);
		}
	}

	@Override
	public T put(Tuple key, T value) {
		checkKey(key);
		var oldValue = map.put(key, value);
		notifyListeners(key, oldValue, value, false);
		return oldValue;
	}

	@Override
	public void putAll(Cursor<Tuple, T> cursor) {
		if (listeners.isEmpty()) {
			map.putAll(cursor);
			return;
		}
		if (cursor.getDependingMaps().contains(map)) {
			List<Tuple> keys = new ArrayList<>();
			List<T> values = new ArrayList<>();
			while (cursor.move()) {
				keys.add(cursor.getKey());
				values.add(cursor.getValue());
			}
			var keyIterator = keys.iterator();
			var valueIterator = values.iterator();
			while (keyIterator.hasNext()) {
				put(keyIterator.next(), valueIterator.next());
			}
		} else {
			while (cursor.move()) {
				put(cursor.getKey(), cursor.getValue());
			}
		}
	}

	@Override
	public DiffCursor<Tuple, T> getDiffCursor(long to) {
		var fromCursor = getAll();
		var toCursor = store.createMap(to).getAll();
		return new MapDiffCursor<>(TupleHashProvider.INSTANCE, symbol.defaultValue(), fromCursor, toCursor);
	}

	public long commit() {
		return map.commit();
	}

	public void restore(long state) {
		if (!restoreListeners.isEmpty()) {
			var diffCursor = getDiffCursor(state);
			while (diffCursor.move()) {
				notifyListeners(diffCursor.getKey(), diffCursor.getFromValue(), diffCursor.getToValue(), true);
			}
		}
		map.restore(state);
	}

	@Override
	public void addListener(InterpretationListener<T> listener, boolean alsoWhenRestoring) {
		listeners.add(listener);
		if (alsoWhenRestoring) {
			restoreListeners.add(listener);
		}
	}

	@Override
	public void removeListener(InterpretationListener<T> listener) {
		listeners.remove(listener);
		restoreListeners.remove(listener);
	}

	static <T> VersionedInterpretation<T> of(ModelImpl model, AnySymbol symbol, VersionedMapStore<Tuple, T> store) {
		@SuppressWarnings("unchecked")
		var typedSymbol = (Symbol<T>) symbol;
		var map = store.createMap();
		return new VersionedInterpretation<>(model, typedSymbol, store, map);
	}

	static <T> VersionedInterpretation<T> of(ModelImpl model, AnySymbol symbol, VersionedMapStore<Tuple, T> store,
											 long state) {
		@SuppressWarnings("unchecked")
		var typedSymbol = (Symbol<T>) symbol;
		var map = store.createMap(state);
		return new VersionedInterpretation<>(model, typedSymbol, store, map);
	}
}
