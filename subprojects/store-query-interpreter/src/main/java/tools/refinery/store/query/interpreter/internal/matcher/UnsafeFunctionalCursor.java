/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

import java.util.Iterator;

/**
 * Cursor for a functional result set that iterates over a stream of raw matches and doesn't check whether the
 * functional dependency of the output on the inputs is obeyed.
 * @param <T> The output type.
 */
class UnsafeFunctionalCursor<T> implements Cursor<Tuple, T> {
	private final Iterator<? extends ITuple> tuplesIterator;
	private boolean terminated;
	private Tuple key;
	private T value;

	public UnsafeFunctionalCursor(Iterator<? extends ITuple> tuplesIterator) {
		this.tuplesIterator = tuplesIterator;
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
		if (!terminated && tuplesIterator.hasNext()) {
			var match = tuplesIterator.next();
			key = MatcherUtils.keyToRefineryTuple(match);
			value = MatcherUtils.getValue(match);
			return true;
		}
		terminated = true;
		return false;
	}
}
