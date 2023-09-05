/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.stateequivalence;

import org.eclipse.collections.api.factory.primitive.IntIntMaps;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;

import java.util.*;

public class CombinationNodePairing implements NodePairing {
	private final int[] left;
	private final int[] right;

	CombinationNodePairing(IntSet left, IntSet right) {
		this.left = left.toArray();
		this.right = right.toArray();

		Arrays.sort(this.left);
		Arrays.sort(this.right);
	}

	@Override
	public int size() {
		return left.length;
	}

	private static final int LIMIT = 5;
	// Enum-based singleton used to delay generating all permutations until they are first needed.
	@SuppressWarnings("squid:S6548")
	private enum PermutationsHolder {
		INSTANCE;

		final CombinationNodePairingPermutations permutations = new CombinationNodePairingPermutations(LIMIT);
	}

	@Override
	public List<IntIntMap> permutations() {
		int limit = this.size();
		Iterable<Integer> interval = () -> new IntervalIterator(limit);

		if (isComplete()) {
			final List<int[]> p = PermutationsHolder.INSTANCE.permutations.getPermutations(this.size() - 1);
			return p.stream().map(x -> constructPermutationMap(interval, x)).toList();
		} else {
			return List.of(constructTrivialMap(interval));
		}
	}

	private IntIntMap constructTrivialMap(Iterable<Integer> interval) {
		return IntIntMaps.immutable.from(interval, l -> left[l], r -> right[r]);
	}

	private IntIntMap constructPermutationMap(Iterable<Integer> interval, int[] permutation) {
		return IntIntMaps.immutable.from(interval, l -> left[l], r -> right[permutation[r]]);
	}

	@Override
	public boolean isComplete() {
		return this.size() <= LIMIT;
	}

	private static class IntervalIterator implements Iterator<Integer> {
		private final int limit;
		private int value = 0;

		private IntervalIterator(int max) {
			this.limit = max;
		}

		@Override
		public boolean hasNext() {
			return value < limit;
		}

		@Override
		public Integer next() {
			if (value >= limit) {
				throw new NoSuchElementException("End of interval");
			}
			int next = value;
			value++;
			return next;
		}
	}
}
