/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

public interface MutableSeed<T> extends Seed<T> {
	void mergeValue(Tuple tuple, T value);

	void setIfMissing(Tuple tuple, T value);

	void setAllMissing(T value);

	void overwriteValues(MutableSeed<T> other);

	static MutableSeed<TruthValue> of(int levels, TruthValue initialValue) {
		if (levels == 0) {
			return new NullaryMutableSeed(initialValue);
		} else {
			return new DecisionTree(levels, initialValue);
		}
	}
}
