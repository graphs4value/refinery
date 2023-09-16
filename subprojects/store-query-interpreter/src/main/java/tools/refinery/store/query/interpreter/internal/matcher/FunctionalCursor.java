/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.rete.index.IterableIndexer;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

import java.util.Iterator;

class FunctionalCursor<T> implements Cursor<Tuple, T> {
	private final IterableIndexer indexer;
	private final Iterator<tools.refinery.interpreter.matchers.tuple.Tuple> iterator;
	private boolean terminated;
	private Tuple key;
	private T value;

	public FunctionalCursor(IterableIndexer indexer) {
		this.indexer = indexer;
		iterator = indexer.getSignatures().iterator();
	}

	@Override
	public Tuple getKey() {
		return key;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public boolean move() {
		if (!terminated && iterator.hasNext()) {
			var match = iterator.next();
			key = MatcherUtils.toRefineryTuple(match);
			value = MatcherUtils.getSingleValue(indexer.get(match));
			return true;
		}
		terminated = true;
		return false;
	}
}
