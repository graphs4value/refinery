/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.Cursors;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

class NullaryMutableSeed implements MutableSeed<TruthValue> {
	private DecisionTreeValue value;

	public NullaryMutableSeed(TruthValue reducedValue) {

		value = DecisionTreeValue.fromTruthValue(reducedValue);
	}

	@Override
	public int arity() {
		return 0;
	}

	@Override
	public Class<TruthValue> valueType() {
		return TruthValue.class;
	}

	@Override
	public TruthValue majorityValue() {
		return value.getTruthValue();
	}

	@Override
	public TruthValue get(Tuple key) {
		validateKey(key);
		return majorityValue();
	}

	private static void validateKey(Tuple key) {
		if (key.getSize() > 0) {
			throw new IllegalArgumentException("Invalid key: " + key);
		}
	}

	@Override
	public Cursor<Tuple, TruthValue> getCursor(TruthValue defaultValue, int nodeCount) {
		if (value == DecisionTreeValue.UNSET || value.getTruthValue() == defaultValue) {
			return Cursors.empty();
		}
		return Cursors.singleton(Tuple.of(), value.getTruthValue());
	}

	@Override
	public void mergeValue(Tuple tuple, TruthValue value) {
		this.value = DecisionTreeValue.fromTruthValue(this.value.merge(value));
	}

	@Override
	public void setIfMissing(Tuple tuple, TruthValue value) {
		validateKey(tuple);
		setAllMissing(value);
	}

	@Override
	public void setAllMissing(TruthValue value) {
		if (this.value == DecisionTreeValue.UNSET) {
			this.value = DecisionTreeValue.fromTruthValue(value);
		}
	}

	@Override
	public void overwriteValues(MutableSeed<TruthValue> other) {
		if (!(other instanceof NullaryMutableSeed nullaryMutableSeed)) {
			throw new IllegalArgumentException("Incompatible overwrite: " + other);
		}
		if (nullaryMutableSeed.value != DecisionTreeValue.UNSET) {
			value = nullaryMutableSeed.value;
		}
	}
}
