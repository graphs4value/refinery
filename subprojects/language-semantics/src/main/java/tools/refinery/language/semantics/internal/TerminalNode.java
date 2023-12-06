/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.representation.TruthValue;

class TerminalNode extends DecisionTreeNode {
	private MutableIntObjectMap<TruthValue> children;

	private DecisionTreeValue otherwise;

	TerminalNode(MutableIntObjectMap<TruthValue> children, DecisionTreeValue otherwise) {
		this.children = children;
		this.otherwise = otherwise;
	}

	private DecisionTreeValue getChild(int index) {
		var child = children.get(index);
		return child == null ? otherwise : DecisionTreeValue.fromTruthValue(child);
	}

	@Override
	public DecisionTreeValue getValue(int level, Tuple tuple) {
		assertLevel(level);
		return getChild(tuple.get(level));
	}

	@Override
	public DecisionTreeNode deepCopy() {
		return new TerminalNode(IntObjectMaps.mutable.ofAll(children), otherwise);
	}

	@Override
	public void mergeValue(int level, Tuple tuple, TruthValue value) {
		assertLevel(level);
		super.mergeValue(level, tuple, value);
	}

	@Override
	protected void mergeAllValues(int nextLevel, Tuple tuple, TruthValue value) {
		otherwise = DecisionTreeValue.fromTruthValue(otherwise.merge(value));
		children = IntObjectMaps.mutable.from(children.keyValuesView(), IntObjectPair::getOne,
				pair -> pair.getTwo().merge(value));
		reduceChildren();
	}

	@Override
	protected void mergeSingleValue(int key, int nextLevel, Tuple tuple, TruthValue value) {
		var newChild = getChild(key).merge(value);
		if (otherwise.getTruthValue() == newChild) {
			children.remove(key);
		} else {
			children.put(key, newChild);
		}
	}

	@Override
	public void setIfMissing(int level, Tuple tuple, TruthValue value) {
		assertLevel(level);
		super.setIfMissing(level, tuple, value);
	}

	@Override
	protected void doSetIfMissing(int key, int nextLevel, Tuple tuple, TruthValue value) {
		if (otherwise == DecisionTreeValue.UNSET) {
			children.getIfAbsentPut(key, value);
		}
	}

	@Override
	public void setAllMissing(TruthValue value) {
		if (otherwise == DecisionTreeValue.UNSET) {
			otherwise = DecisionTreeValue.fromTruthValue(value);
			reduceChildren();
		}
	}

	@Override
	public void overwriteValues(DecisionTreeNode values) {
		if (!(values instanceof TerminalNode terminalValues)) {
			throw new IllegalArgumentException("Level mismatch");
		}
		otherwise = otherwise.overwrite(terminalValues.otherwise);
		children = IntObjectMaps.mutable.from(children.keyValuesView(), IntObjectPair::getOne,
				pair -> terminalValues.getChild(pair.getOne()).getTruthValueOrElse(pair.getTwo()));
		for (var pair : terminalValues.children.keyValuesView()) {
			children.getIfAbsentPut(pair.getOne(), pair.getTwo());
		}
		reduceChildren();
	}

	private void reduceChildren() {
		var iterator = children.iterator();
		while (iterator.hasNext()) {
			var child = iterator.next();
			if (otherwise.getTruthValue() == child) {
				iterator.remove();
			}
		}
	}

	@Override
	public boolean moveNext(int level, DecisionTreeCursor cursor) {
		assertLevel(level);
		return super.moveNext(level, cursor);
	}

	@Override
	protected DecisionTreeValue getOtherwiseReducedValue() {
		return getMajorityValue();
	}

	@Override
	protected LazyIntIterable getChildKeys() {
		return children.keysView();
	}

	@Override
	public DecisionTreeValue getMajorityValue() {
		return otherwise;
	}

	@Override
	protected boolean moveNextSparse(int level, DecisionTreeCursor cursor, int startIndex, int[] sortedChildren) {
		if (startIndex >= sortedChildren.length) {
			return false;
		}
		var key = sortedChildren[startIndex];
		cursor.iterationState[level] = startIndex;
		cursor.rawTuple[level] = key;
		cursor.value = getChild(key);
		return true;
	}

	@Override
	protected boolean moveNextDense(int level, DecisionTreeCursor cursor, int startIndex) {
		if (startIndex >= cursor.nodeCount) {
			return false;
		}
		cursor.iterationState[level] = startIndex;
		cursor.rawTuple[level] = startIndex;
		cursor.value = getChild(startIndex);
		return true;
	}

	private static void assertLevel(int level) {
		if (level != 0) {
			throw new IllegalArgumentException("Invalid decision tree level: " + level);
		}
	}
}
