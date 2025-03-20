/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.logic.dnf.Query;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.OrderedResultSet;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class PriorityResultSet<T> implements OrderedResultSet<T> {
	private final OrderedResultSet<T> resultSet;
	private final int priority;
	private final PriorityAgenda agenda;
	private final ResultSetListener<T> listener;

	public PriorityResultSet(OrderedResultSet<T> resultSet, int priority, PriorityAgenda agenda) {
		this.resultSet = resultSet;
		this.priority = priority;
		this.agenda = agenda;
		var defaultValue = getCanonicalQuery().defaultValue();
		listener =  (key, fromValue, toValue) -> {
			int size = resultSet.size();
			if (size == 0 && Objects.equals(toValue, defaultValue)) {
				// Result set just became empty, since the match for {@code key} has just disappeared.
				agenda.removeResultSet(this);
			} else if (size == 1 && Objects.equals(fromValue, defaultValue)) {
				// Result set just became non-empty, since the match for {@code key} has just appeared.
				agenda.addResultSet(this);
			}
		};
		if (resultSet.size() > 0) {
			agenda.addResultSet(this);
		}
		resultSet.addListener(listener);
	}

	@Override
	public ModelQueryAdapter getAdapter() {
		return resultSet.getAdapter();
	}

	public int getPriority() {
		return priority;
	}

	@Override
	public Query<T> getCanonicalQuery() {
		return resultSet.getCanonicalQuery();
	}

	private boolean isEnabled() {
		return agenda.isEnabled(priority);
	}

	@Override
	public int size() {
		return isEnabled() ? resultSet.size() : 0;
	}

	@Override
	public T get(Tuple parameters) {
		return isEnabled() ? resultSet.get(parameters) : getCanonicalQuery().defaultValue();
	}

	@Override
	public Cursor<Tuple, T> getAll() {
		return isEnabled() ? resultSet.getAll() : Cursors.empty();
	}

	@Override
	public Tuple getKey(int index) {
		if (!isEnabled()) {
			throw new IndexOutOfBoundsException("Result set disabled by priority");
		}
		return resultSet.getKey(index);
	}

	@Override
	public void addListener(ResultSetListener<T> listener) {
		resultSet.addListener(listener);
	}

	@Override
	public void removeListener(ResultSetListener<T> listener) {
		resultSet.removeListener(listener);
	}

	@Override
	public void close() throws Exception {
		resultSet.removeListener(listener);
		resultSet.close();
	}
}
