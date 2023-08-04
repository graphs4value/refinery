/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.stateequivalence;

import org.eclipse.collections.api.factory.primitive.IntIntMaps;
import org.eclipse.collections.api.map.primitive.IntIntMap;

import java.util.List;

public class TrivialNodePairing implements NodePairing {
	final int left;
	final int right;

	TrivialNodePairing(int left, int right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public List<IntIntMap> permutations() {
		return List.of(IntIntMaps.immutable.of(left,right));
	}

	@Override
	public boolean isComplete() {
		return true;
	}
}
