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

final class IntermediateNode<A extends AbstractValue<A, C>, C> extends DecisionTreeNode<A, C> {
	private final MutableIntObjectMap<DecisionTreeNode<A, C>> children;

	private final DecisionTreeNode<A, C> otherwise;

	IntermediateNode(MutableIntObjectMap<DecisionTreeNode<A, C>> children, DecisionTreeNode<A, C> otherwise) {
		this.children = children;
		this.otherwise = otherwise;
	}

	private DecisionTreeNode<A, C> getChild(int index) {
		var child = children.get(index);
		return child == null ? otherwise : child;
	}

	@Override
	public DecisionTreeValue<A, C> getValue(int level, Tuple tuple) {
		return getChild(tuple.get(level)).getValue(level - 1, tuple);
	}

	@Override
	public DecisionTreeNode<A, C> deepCopy() {
		var newChildren = IntObjectMaps.mutable.from(children.keyValuesView(), IntObjectPair::getOne,
				pair -> pair.getTwo().deepCopy());
		var newOtherwise = otherwise.deepCopy();
		return new IntermediateNode<>(newChildren, newOtherwise);
	}

	@Override
	protected void mergeAllValues(int nextLevel, Tuple tuple, A value) {
		otherwise.mergeValue(nextLevel, tuple, value);
		for (var child : children) {
			child.mergeValue(nextLevel, tuple, value);
		}
		reduceChildren();
	}

	@Override
	protected void mergeSingleValue(int key, int nextLevel, Tuple tuple, A value) {
		var otherwiseReduced = getOtherwiseReducedValue();
		var child = children.get(key);
		if (child == null) {
			var newChild = otherwise.withMergedValue(nextLevel, tuple, value);
			if (otherwiseReduced == null || !Objects.equals(newChild.getReducedValue(), otherwiseReduced)) {
				children.put(key, newChild);
			}
			return;
		}
		child.mergeValue(nextLevel, tuple, value);
		if (otherwiseReduced != null && Objects.equals(child.getReducedValue(), otherwiseReduced)) {
			children.remove(key);
		}
	}

	@Override
	protected void doSetIfMissing(int key, int nextLevel, Tuple tuple, A value) {
		var child = children.get(key);
		if (child == null) {
			var otherwiseReducedValue = getOtherwiseReducedValue();
			if (otherwiseReducedValue != null && !otherwiseReducedValue.isUnset()) {
				// Value already set.
				return;
			}
			var newChild = otherwise.withValueSetIfMissing(nextLevel, tuple, value);
			children.put(key, newChild);
			return;
		}
		child.setIfMissing(nextLevel, tuple, value);
	}

	@Override
	public void setAllMissing(A value) {
		otherwise.setAllMissing(value);
		for (var child : children) {
			child.setAllMissing(value);
		}
		reduceChildren();
	}

	@Override
	public void overwriteValues(DecisionTreeNode<A, C> values) {
		if (!(values instanceof IntermediateNode<A, C> intermediateValues)) {
			throw new IllegalArgumentException("Level mismatch");
		}
		otherwise.overwriteValues(intermediateValues.otherwise);
		for (var pair : children.keyValuesView()) {
			pair.getTwo().overwriteValues(intermediateValues.getChild(pair.getOne()));
		}
		for (var pair : intermediateValues.children.keyValuesView()) {
			children.getIfAbsentPut(pair.getOne(), () -> otherwise.withOverwrittenValues(pair.getTwo()));
		}
		reduceChildren();
	}

	private void reduceChildren() {
		var otherwiseReduced = getOtherwiseReducedValue();
		if (otherwiseReduced == null) {
			return;
		}
		var iterator = children.iterator();
		while (iterator.hasNext()) {
			var child = iterator.next();
			if (Objects.equals(child.getReducedValue(), otherwiseReduced)) {
				iterator.remove();
			}
		}
	}

	@Override
	protected DecisionTreeValue<A, C> getOtherwiseReducedValue() {
		return otherwise.getReducedValue();
	}

	@Override
	protected LazyIntIterable getChildKeys() {
		return children.keysView();
	}

	@Override
	public DecisionTreeValue<A, C> getMajorityValue() {
		return otherwise.getMajorityValue();
	}

	protected boolean moveNextSparse(int level, DecisionTreeCursor<A, C> cursor, int startIndex, int[] sortedChildren) {
		for (int i = startIndex; i < sortedChildren.length; i++) {
			var key = sortedChildren[i];
			var child = getChild(key);
			var found = child.moveNext(level - 1, cursor);
			if (found) {
				cursor.iterationState[level] = i;
				cursor.rawTuple[level] = key;
				return true;
			}
		}
		return false;
	}

	protected boolean moveNextDense(int level, DecisionTreeCursor<A, C> cursor, int startIndex) {
		for (int i = startIndex; i < cursor.nodeCount; i++) {
			var child = getChild(i);
			var found = child.moveNext(level - 1, cursor);
			if (found) {
				cursor.iterationState[level] = i;
				cursor.rawTuple[level] = i;
				return true;
			}
		}
		return false;
	}
}
