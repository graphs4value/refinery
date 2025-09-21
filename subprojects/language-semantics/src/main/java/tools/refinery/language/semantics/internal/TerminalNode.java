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
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

class TerminalNode<A extends AbstractValue<A, C>, C> extends DecisionTreeNode<A, C> {
	private MutableIntObjectMap<A> children;

	private DecisionTreeValue<A, C> otherwise;

	TerminalNode(MutableIntObjectMap<A> children, DecisionTreeValue<A, C> otherwise) {
		this.children = children;
		this.otherwise = otherwise;
	}

	private DecisionTreeValue<A, C> getChild(int index) {
		var child = children.get(index);
		return child == null ? otherwise : DecisionTreeValue.ofNullable(child);
	}

	@Override
	public DecisionTreeValue<A, C> getValue(int level, Tuple tuple) {
		assertLevel(level);
		return getChild(tuple.get(level));
	}

	@Override
	public DecisionTreeNode<A, C> deepCopy() {
		return new TerminalNode<>(IntObjectMaps.mutable.ofAll(children), otherwise);
	}

	@Override
	public void mergeValue(int level, Tuple tuple, A value) {
		assertLevel(level);
		super.mergeValue(level, tuple, value);
	}

	@Override
	protected void mergeAllValues(int nextLevel, Tuple tuple, A value) {
		otherwise = DecisionTreeValue.ofNullable(otherwise.merge(value));
		children = IntObjectMaps.mutable.from(children.keyValuesView(), IntObjectPair::getOne,
				pair -> pair.getTwo().meet(value));
		reduceChildren();
	}

	@Override
	protected void mergeSingleValue(int key, int nextLevel, Tuple tuple, A value) {
		var newChild = getChild(key).merge(value);
		if (Objects.equals(otherwise.orElseNull(), newChild)) {
			children.remove(key);
		} else {
			children.put(key, newChild);
		}
	}

	@Override
	public void setIfMissing(int level, Tuple tuple, A value) {
		assertLevel(level);
		super.setIfMissing(level, tuple, value);
	}

	@Override
	protected void doSetIfMissing(int key, int nextLevel, Tuple tuple, A value) {
		if (otherwise.isUnset()) {
			children.getIfAbsentPut(key, value);
		}
	}

	@Override
	public void setAllMissing(A value) {
		if (otherwise.isUnset()) {
			otherwise = DecisionTreeValue.ofNullable(value);
			reduceChildren();
		}
	}

	@Override
	public void overwriteValues(DecisionTreeNode<A, C> values) {
		if (!(values instanceof TerminalNode<A, C> terminalValues)) {
			throw new IllegalArgumentException("Level mismatch");
		}
		otherwise = otherwise.overwrite(terminalValues.otherwise);
		children = IntObjectMaps.mutable.from(children.keyValuesView(), IntObjectPair::getOne,
				pair -> terminalValues.getChild(pair.getOne()).orElse(pair.getTwo()));
		for (var pair : terminalValues.children.keyValuesView()) {
			children.getIfAbsentPut(pair.getOne(), pair.getTwo());
		}
		reduceChildren();
	}

	private void reduceChildren() {
		var iterator = children.iterator();
		while (iterator.hasNext()) {
			var child = iterator.next();
			if (Objects.equals(otherwise.orElseNull(), child)) {
				iterator.remove();
			}
		}
	}

	@Override
	public boolean moveNext(int level, DecisionTreeCursor<A, C> cursor) {
		assertLevel(level);
		return super.moveNext(level, cursor);
	}

	@Override
	protected DecisionTreeValue<A, C> getOtherwiseReducedValue() {
		return getMajorityValue();
	}

	@Override
	protected LazyIntIterable getChildKeys() {
		return children.keysView();
	}

	@Override
	public DecisionTreeValue<A, C> getMajorityValue() {
		return otherwise;
	}

	@Override
	protected boolean moveNextSparse(int level, DecisionTreeCursor<A, C> cursor, int startIndex, int[] sortedChildren) {
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
	protected boolean moveNextDense(int level, DecisionTreeCursor<A, C> cursor, int startIndex) {
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
