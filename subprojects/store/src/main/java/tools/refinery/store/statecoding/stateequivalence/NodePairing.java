/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.stateequivalence;

import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;

import java.util.List;

public interface NodePairing {
	int size();

	List<IntIntMap> permutations();

	boolean isComplete();

	static NodePairing constructNodePairing(IntSet left, IntSet right){
		if(left.size() !=  right.size()) {
			return null;
		}

		if(left.size() == 1) {
			int leftValue = left.intIterator().next();
			int rightValue = right.intIterator().next();
			return new TrivialNodePairing(leftValue, rightValue);
		} else {
			return new CombinationNodePairing(left,right);
		}
	}
}
