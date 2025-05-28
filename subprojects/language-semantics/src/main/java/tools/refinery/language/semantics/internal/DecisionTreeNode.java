/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import org.eclipse.collections.api.LazyIntIterable;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.tuple.Tuple;

abstract class DecisionTreeNode<A extends AbstractValue<A, C>, C> {
	public DecisionTreeValue<A, C> getReducedValue() {
		return getChildKeys().isEmpty() ? getOtherwiseReducedValue() : null;
	}

	public abstract DecisionTreeValue<A, C> getValue(int level, Tuple tuple);

	public abstract DecisionTreeNode<A, C> deepCopy();

	public void mergeValue(int level, Tuple tuple, A value) {
		int nextLevel = level - 1;
		var key = tuple.get(level);
		if (key < 0) {
			mergeAllValues(nextLevel, tuple, value);
		} else {
			mergeSingleValue(key, nextLevel, tuple, value);
		}
	}

	protected abstract void mergeAllValues(int nextLevel, Tuple tuple, A value);

	protected abstract void mergeSingleValue(int key, int nextLevel, Tuple tuple, A value);

	public DecisionTreeNode<A, C> withMergedValue(int level, Tuple tuple, A value) {
		var copy = deepCopy();
		copy.mergeValue(level, tuple, value);
		return copy;
	}

	public void setIfMissing(int level, Tuple tuple, A value) {
		var key = tuple.get(level);
		if (key < 0) {
			throw new IllegalArgumentException("Not allowed set a missing wildcard");
		}
		doSetIfMissing(key, level - 1, tuple, value);
	}

	protected abstract void doSetIfMissing(int key, int nextLevel, Tuple tuple, A value);

	public DecisionTreeNode<A, C> withValueSetIfMissing(int level, Tuple tuple, A value) {
		var copy = deepCopy();
		copy.setIfMissing(level, tuple, value);
		return copy;
	}

	public abstract void setAllMissing(A value);

	public abstract void overwriteValues(DecisionTreeNode<A, C> values);

	public DecisionTreeNode<A, C> withOverwrittenValues(DecisionTreeNode<A, C> values) {
		var copy = deepCopy();
		copy.overwriteValues(values);
		return copy;
	}

	public boolean moveNext(int level, DecisionTreeCursor<A, C> cursor) {
		var currentState = cursor.iterationState[level];
		boolean found;
		if (currentState == DecisionTreeCursor.STATE_FINISH) {
			// Entering this node for the first time.
			cursor.path.push(this);
			if (cursor.defaultValue == getOtherwiseReducedValue()) {
				var sortedChildren = getChildKeys().toSortedArray();
				cursor.sortedChildren[level] = sortedChildren;
				found = moveNextSparse(level, cursor, 0, sortedChildren);
			} else {
				found = moveNextDense(level, cursor, 0);
			}
		} else {
			var sortedChildren = cursor.sortedChildren[level];
			if (sortedChildren == null) {
				found = moveNextDense(level, cursor, currentState + 1);
			} else {
				found = moveNextSparse(level, cursor, currentState + 1, sortedChildren);
			}
		}
		if (!found) {
			cursor.sortedChildren[level] = null;
			cursor.iterationState[level] = DecisionTreeCursor.STATE_FINISH;
			var popped = cursor.path.pop();
			if (popped != this) {
				throw new IllegalStateException("Invalid decision diagram cursor");
			}
		}
		return found;
	}

	protected abstract DecisionTreeValue<A, C> getOtherwiseReducedValue();

	protected abstract LazyIntIterable getChildKeys();

	public abstract DecisionTreeValue<A, C> getMajorityValue();

	protected abstract boolean moveNextSparse(int level, DecisionTreeCursor<A, C> cursor, int startIndex,
											  int[] sortedChildren);

	protected abstract boolean moveNextDense(int level, DecisionTreeCursor<A, C> cursor, int startIndex);
}
