/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.stateequivalence;

import java.util.ArrayList;
import java.util.List;

class CombinationNodePairingPermutations {
	private final List<List<int[]>> permutations = new ArrayList<>();

	public CombinationNodePairingPermutations(int max) {
		initializePermutations(max);
	}

	public List<int[]> getPermutations(int max) {
		if (max >= permutations.size()) {
			throw new IllegalArgumentException("Only permutations up to %d elements are supported".formatted(max));
		}
		return permutations.get(max);
	}

	/**
	 * Generates and stores permutations up to a given size. If the number would be more than a limit, it provides a
	 * single permutation only.
	 *
	 * @param max The max number in the permutation
	 * @return A complete list of permutations of numbers 0...max, or a single permutation.
	 */
	private List<int[]> initializePermutations(int max) {
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
		List<int[]> previousPermutations = initializePermutations(max - 1);
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
}
