/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueDomain;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.tuple.Tuple;

public interface MutableSeed<T> extends Seed<T> {
	void mergeValue(Tuple tuple, T value);

	void setIfMissing(Tuple tuple, T value);

	void setAllMissing(T value);

	void overwriteValues(MutableSeed<T> other);

	static <A extends AbstractValue<A, C>, C> MutableSeed<A> of(int levels, AbstractDomain<A, C> domain,
																A fallbackMajorityValue, A initialValue) {
		if (levels == 0) {
			return new NullaryMutableSeed<>(domain.abstractType(),
					initialValue == null ? fallbackMajorityValue : initialValue);
		} else {
			return new DecisionTree<>(levels, domain.abstractType(), fallbackMajorityValue, initialValue);
		}
	}

	static <A extends AbstractValue<A, C>, C> MutableSeed<A> of(int levels, AbstractDomain<A, C> domain,
																A initialValue) {
		return of(levels, domain, domain.unknown(), initialValue);
	}

	static MutableSeed<TruthValue> of(int levels, TruthValue initialValue) {
		return of(levels, TruthValueDomain.INSTANCE, TruthValue.FALSE, initialValue);
	}
}
