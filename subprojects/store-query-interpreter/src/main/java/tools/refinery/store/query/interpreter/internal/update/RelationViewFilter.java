/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.update;

import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContextListener;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;

import java.util.Arrays;
import java.util.Objects;

public final class RelationViewFilter {
	private final IInputKey inputKey;
	private final Object[] seed;
	private final IQueryRuntimeContextListener listener;

	public RelationViewFilter(IInputKey inputKey, ITuple seed, IQueryRuntimeContextListener listener) {
		this.inputKey = inputKey;
		this.seed = seedToArray(seed);
		this.listener = listener;
	}

	public void update(Tuple updateTuple, boolean isInsertion) {
		if (isMatching(updateTuple)) {
			listener.update(inputKey, updateTuple, isInsertion);
		}
	}

	private boolean isMatching(ITuple tuple) {
		if (seed == null) {
			return true;
		}
		int size = seed.length;
		for (int i = 0; i < size; i++) {
			var filterElement = seed[i];
			if (filterElement != null && !filterElement.equals(tuple.get(i))) {
				return false;
			}
		}
		return true;
	}

	// Use <code>null</code> instead of an empty array to speed up comparisons.
	@SuppressWarnings("squid:S1168")
	private static Object[] seedToArray(ITuple seed) {
		for (var element : seed.getElements()) {
			if (element != null) {
				return seed.getElements();
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (RelationViewFilter) obj;
		return Objects.equals(this.inputKey, that.inputKey) && Arrays.equals(this.seed, that.seed) &&
				Objects.equals(this.listener, that.listener);
	}

	@Override
	public int hashCode() {
		return Objects.hash(inputKey, Arrays.hashCode(seed), listener);
	}
}
