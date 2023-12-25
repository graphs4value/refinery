/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

class DecisionTree implements MutableSeed<TruthValue> {
	private final int levels;

	private final DecisionTreeNode root;

	public DecisionTree(int levels, TruthValue initialValue) {
		this.levels = levels;
		DecisionTreeNode node = new TerminalNode(IntObjectMaps.mutable.empty(),
				DecisionTreeValue.fromTruthValue(initialValue));
		for (int level = 1; level < levels; level++) {
			node = new IntermediateNode(IntObjectMaps.mutable.empty(), node);
		}
		root = node;
	}

	public DecisionTree(int levels) {
		this(levels, null);
	}

	@Override
	public int arity() {
		return levels;
	}

	@Override
	public Class<TruthValue> valueType() {
		return TruthValue.class;
	}

	@Override
	public TruthValue majorityValue() {
		return root.getMajorityValue().getTruthValueOrElse(TruthValue.FALSE);
	}

	@Override
	public TruthValue get(Tuple tuple) {
		return root.getValue(levels - 1, tuple).getTruthValue();
	}

	@Override
	public void mergeValue(Tuple tuple, TruthValue truthValue) {
		if (truthValue != null) {
			root.mergeValue(levels - 1, tuple, truthValue);
		}
	}

	@Override
	public void setIfMissing(Tuple tuple, TruthValue truthValue) {
		if (truthValue != null) {
			root.setIfMissing(levels - 1, tuple, truthValue);
		}
	}

	@Override
	public void setAllMissing(TruthValue truthValue) {
		if (truthValue != null) {
			root.setAllMissing(truthValue);
		}
	}

	@Override
	public void overwriteValues(MutableSeed<TruthValue> values) {
		if (!(values instanceof DecisionTree decisionTree)) {
			throw new IllegalArgumentException("Incompatible overwrite: " + values);
		}
		root.overwriteValues(decisionTree.root);
	}

	public TruthValue getReducedValue() {
		var reducedValue = root.getReducedValue();
		return reducedValue == null ? null : reducedValue.getTruthValue();
	}

	@Override
	public Cursor<Tuple, TruthValue> getCursor(TruthValue defaultValue, int nodeCount) {
		return new DecisionTreeCursor(levels, defaultValue, nodeCount, root);
	}
}
