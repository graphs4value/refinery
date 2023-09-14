/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractResultSet<T> implements ResultSet<T> {
	private final ModelQueryAdapter adapter;
	private final Query<T> query;
	private final List<ResultSetListener<T>> listeners = new ArrayList<>();

	protected AbstractResultSet(ModelQueryAdapter adapter, Query<T> query) {
		this.adapter = adapter;
		this.query = query;
	}

	@Override
	public ModelQueryAdapter getAdapter() {
		return adapter;
	}

	@Override
	public Query<T> getCanonicalQuery() {
		return query;
	}

	@Override
	public void addListener(ResultSetListener<T> listener) {
		if (listeners.isEmpty()) {
			startListeningForChanges();
		}
		listeners.add(listener);
	}

	@Override
	public void removeListener(ResultSetListener<T> listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			stopListeningForChanges();
		}
	}

	protected abstract void startListeningForChanges();

	protected abstract void stopListeningForChanges();

	protected void notifyChange(Tuple key, T oldValue, T newValue) {
		int listenerCount = listeners.size();
		// Use a for loop instead of a for-each loop to avoid {@code Iterator} allocation overhead.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < listenerCount; i++) {
			listeners.get(i).put(key, oldValue, newValue);
		}
	}
}
