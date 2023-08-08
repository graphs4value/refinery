/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.stateequivalence;

import org.eclipse.collections.api.factory.primitive.IntIntMaps;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.list.Interval;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombinationNodePairing implements NodePairing {
	private final int[] left;
	private final int[] right;

	CombinationNodePairing(IntSet left, IntHashSet right) {
		this.left = left.toArray();
		this.right = right.toArray();

		Arrays.sort(this.left);
		Arrays.sort(this.right);
	}

	@Override
	public int size() {
		return left.length;
	}

	static final int LIMIT = 5;
	static final List<List<int[]>> permutations = new ArrayList<>();

	/**
	 * Generates and stores permutations up to a given size. If the number would be more than a limit, it provides a
	 * single permutation only.
	 *
	 * @param max The max number in the permutation
	 * @return A complete list of permutations of numbers 0...max, or a single permutation.
	 */
	public static List<int[]> getPermutations(int max) {
		if (max < permutations.size()) {
			return permutations.get(max);
		}
		if (max == 0) {
			List<int[]> result = new ArrayList<>();
			result.add(new int[1]);
			permutations.add(result);
			return result;
		}
		List<int[]> result = new ArrayList<>();
		List<int[]> previousPermutations = getPermutations(max - 1);
		for (var permutation : previousPermutations) {
			for (int pos = 0; pos <= max; pos++) {
				int[] newPermutation = new int[max + 1];
				System.arraycopy(permutation, 0, newPermutation, 0, pos);
				newPermutation[pos] = max;
				if (max - (pos + 1) >= 0)
					System.arraycopy(permutation, pos + 1, newPermutation, pos + 1 + 1, max - (pos + 1));
				result.add(newPermutation);
			}
		}
		permutations.add(result);
		return result;
	}

	@Override
	public List<IntIntMap> permutations() {
		final var interval = Interval.zeroTo(this.size() - 1);

		if (isComplete()) {
			final List<int[]> p = getPermutations(this.size() - 1);
			return p.stream().map(x -> constructPermutationMap(interval, x)).toList();
		} else {
			return List.of(constructTrivialMap(interval));
		}
	}

	private IntIntMap constructTrivialMap(Interval interval) {
		return IntIntMaps.immutable.from(interval, l -> left[l], r -> right[r]);
	}

	private IntIntMap constructPermutationMap(Interval interval, int[] permutation) {
		return IntIntMaps.immutable.from(interval, l -> left[l], r -> right[permutation[r]]);
	}

	@Override
	public boolean isComplete() {
		return this.size() <= LIMIT;
	}
}
