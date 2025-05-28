/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayDeque;
import java.util.Deque;

class DecisionTreeCursor<A extends AbstractValue<A, C>, C> implements Cursor<Tuple, A> {
	static final int STATE_FINISH = Integer.MAX_VALUE;

	private final int levels;

	final DecisionTreeValue<A, C> defaultValue;

	final int nodeCount;

	private final DecisionTreeNode<A, C> root;

	final int[][] sortedChildren;

	final int[] iterationState;

	final int[] rawTuple;

	final Deque<DecisionTreeNode<A, C>> path = new ArrayDeque<>();

	private Tuple key;

	DecisionTreeValue<A, C> value = DecisionTreeValue.unset();

	private boolean terminated;

	public DecisionTreeCursor(int levels, A defaultValue, int nodeCount, DecisionTreeNode<A, C> root) {
		this.levels = levels;
		this.defaultValue = DecisionTreeValue.ofNullable(defaultValue);
		this.nodeCount = nodeCount;
		this.root = root;
		sortedChildren = new int[levels][];
		iterationState = new int[levels];
		for (int i = 0; i < levels; i++) {
			iterationState[i] = STATE_FINISH;
		}
		rawTuple = new int[levels];
	}

	@Override
	public Tuple getKey() {
		return key;
	}

	@Override
	public A getValue() {
		return value.orElseNull();
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public boolean move() {
		while (moveOne()) {
			if (!value.equals(defaultValue)) {
				return true;
			}
		}
		return false;
	}

	private boolean moveOne() {
		boolean found = false;
		if (path.isEmpty() && !terminated) {
			found = root.moveNext(levels - 1, this);
		}
		while (!found && !path.isEmpty()) {
			int level = levels - path.size();
			found = path.peek().moveNext(level, this);
		}
		if (!found) {
			key = null;
			value = DecisionTreeValue.unset();
			terminated = true;
			return false;
		}
		key = Tuple.of(rawTuple);
		return true;
	}
}
