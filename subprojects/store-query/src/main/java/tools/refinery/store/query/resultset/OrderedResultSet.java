/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.resultset;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.utils.OrderStatisticTree;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class OrderedResultSet<T> implements AutoCloseable, ResultSet<T> {
	private final ResultSet<T> resultSet;
	private final OrderStatisticTree<Tuple> tree = new OrderStatisticTree<>();
	private final ResultSetListener<T> listener = (key, fromValue, toValue) -> {
		var defaultValue = getCanonicalQuery().defaultValue();
		if (Objects.equals(defaultValue, toValue)) {
			tree.remove(key);
		} else {
			tree.add(key);
		}
	};

	public OrderedResultSet(ResultSet<T> resultSet) {
		this.resultSet = resultSet;
		resultSet.addListener(listener);
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			tree.add(cursor.getKey());
		}
	}

	@Override
	public ModelQueryAdapter getAdapter() {
		return resultSet.getAdapter();
	}

	@Override
	public int size() {
		return resultSet.size();
	}

	@Override
	public Query<T> getCanonicalQuery() {
		return resultSet.getCanonicalQuery();
	}

	@Override
	public T get(Tuple parameters) {
		return resultSet.get(parameters);
	}

	public Tuple getKey(int index) {
		return tree.get(index);
	}

	@Override
	public Cursor<Tuple, T> getAll() {
		return resultSet.getAll();
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
	public void close() {
		resultSet.removeListener(listener);
	}
}
