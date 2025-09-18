/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

class NullaryMutableSeed<A extends AbstractValue<A, C>, C> implements MutableSeed<A> {
	private final Class<A> valueType;
	private final A fallbackMajorityValue;
	private DecisionTreeValue<A, C> value;

	public NullaryMutableSeed(Class<A> valueType, A fallbackMajorityValue, A reducedValue) {
		this.valueType = valueType;
		this.fallbackMajorityValue = fallbackMajorityValue;
		value = DecisionTreeValue.ofNullable(reducedValue);
	}

	@Override
	public int arity() {
		return 0;
	}

	@Override
	public Class<A> valueType() {
		return valueType;
	}

	@Override
	public A majorityValue() {
		var majorityValue = getValue();
		return majorityValue == null ? fallbackMajorityValue : majorityValue;
	}

	private A getValue() {
		return value.orElseNull();
	}

	@Override
	public A get(Tuple key) {
		validateKey(key);
		return getValue();
	}

	private static void validateKey(Tuple key) {
		if (key.getSize() > 0) {
			throw new IllegalArgumentException("Invalid key: " + key);
		}
	}

	@Override
	public Cursor<Tuple, A> getCursor(A defaultValue, int nodeCount) {
		if (value.isUnset() || Objects.equals(value.orElseNull(), defaultValue)) {
			return Cursors.empty();
		}
		return Cursors.singleton(Tuple.of(), value.orElseNull());
	}

	@Override
	public void mergeValue(Tuple tuple, A value) {
		validateKey(tuple);
		this.value = DecisionTreeValue.ofNullable(this.value.merge(value));
	}

	@Override
	public void setIfMissing(Tuple tuple, A value) {
		validateKey(tuple);
		setAllMissing(value);
	}

	@Override
	public void setAllMissing(A value) {
		if (this.value.isUnset()) {
			this.value = DecisionTreeValue.ofNullable(value);
		}
	}

	@Override
	public void overwriteValues(MutableSeed<A> other) {
		if (!(other instanceof NullaryMutableSeed<?, ?> nullaryMutableSeed)) {
			throw new IllegalArgumentException("Incompatible overwrite: " + other);
		}
		// This is safe, because A uniquely determines C.
		@SuppressWarnings("unchecked")
		var typedValue = (DecisionTreeValue<A, C>) nullaryMutableSeed.value;
		value = value.overwrite(typedValue);
	}
}
