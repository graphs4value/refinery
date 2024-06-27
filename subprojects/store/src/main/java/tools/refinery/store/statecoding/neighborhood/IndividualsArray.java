/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.neighborhood;

import java.util.Arrays;
import java.util.stream.IntStream;

public class IndividualsArray implements IndividualsSet {
	private final int[] sortedArray;

	public IndividualsArray(int[] sortedArray) {
		this.sortedArray = sortedArray;
	}

	@Override
	public boolean isIndividual(int nodeId) {
		return Arrays.binarySearch(sortedArray, nodeId) >= 0;
	}

	@Override
	public IntStream stream() {
		return Arrays.stream(sortedArray);
	}
}
