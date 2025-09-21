/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.tuple.Tuple;

class DecisionTree<A extends AbstractValue<A, C>, C> implements MutableSeed<A> {
	private final int levels;
	private final Class<A> valueType;
	private final A fallbackMajorityValue;
	private final DecisionTreeNode<A, C> root;

	public DecisionTree(int levels, Class<A> valueType, A fallbackMajorityValue, A initialValue) {
		this.levels = levels;
		this.valueType = valueType;
		this.fallbackMajorityValue = fallbackMajorityValue;
		DecisionTreeNode<A, C> node = new TerminalNode<>(IntObjectMaps.mutable.empty(),
				DecisionTreeValue.ofNullable(initialValue));
		for (int level = 1; level < levels; level++) {
			node = new IntermediateNode<>(IntObjectMaps.mutable.empty(), node);
		}
		root = node;
	}

	public DecisionTree(int levels, Class<A> valueType, A fallbackMajorityValue) {
		this(levels, valueType, fallbackMajorityValue, null);
	}

	@Override
	public int arity() {
		return levels;
	}

	@Override
	public Class<A> valueType() {
		return valueType;
	}

	@Override
	public A majorityValue() {
		return root.getMajorityValue().orElse(fallbackMajorityValue);
	}

	@Override
	public A get(Tuple tuple) {
		return root.getValue(levels - 1, tuple).orElseNull();
	}

	@Override
	public void mergeValue(Tuple tuple, A truthValue) {
		if (truthValue != null) {
			root.mergeValue(levels - 1, tuple, truthValue);
		}
	}

	@Override
	public void setIfMissing(Tuple tuple, A truthValue) {
		if (truthValue != null) {
			root.setIfMissing(levels - 1, tuple, truthValue);
		}
	}

	@Override
	public void setAllMissing(A truthValue) {
		if (truthValue != null) {
			root.setAllMissing(truthValue);
		}
	}

	@Override
	public void overwriteValues(MutableSeed<A> values) {
		if (!(values instanceof DecisionTree<?, ?> decisionTree)) {
			throw new IllegalArgumentException("Incompatible overwrite: " + values);
		}
		// This is safe, because A uniquely determines C.
		@SuppressWarnings("unchecked")
		var typedRoot = (DecisionTreeNode<A, C>) decisionTree.root;
		root.overwriteValues(typedRoot);
	}

	public A getReducedValue() {
		var reducedValue = root.getReducedValue();
		return reducedValue == null ? null : reducedValue.orElseNull();
	}

	@Override
	public Cursor<Tuple, A> getCursor(A defaultValue, int nodeCount) {
		return new DecisionTreeCursor<>(levels, defaultValue, nodeCount, root);
	}
}
