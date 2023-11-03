/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayDeque;
import java.util.Deque;

class DecisionTreeCursor implements Cursor<Tuple, TruthValue> {
	static final int STATE_FINISH = Integer.MAX_VALUE;

	private final int levels;

	final DecisionTreeValue defaultValue;

	final int nodeCount;

	private final DecisionTreeNode root;

	final int[][] sortedChildren;

	final int[] iterationState;

	final int[] rawTuple;

	final Deque<DecisionTreeNode> path = new ArrayDeque<>();

	private Tuple key;

	DecisionTreeValue value = DecisionTreeValue.UNSET;

	private boolean terminated;

	public DecisionTreeCursor(int levels, TruthValue defaultValue, int nodeCount, DecisionTreeNode root) {
		this.levels = levels;
		this.defaultValue = DecisionTreeValue.fromTruthValue(defaultValue);
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
	public TruthValue getValue() {
		return value.getTruthValue();
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
			value = DecisionTreeValue.UNSET;
			terminated = true;
			return false;
		}
		key = Tuple.of(rawTuple);
		return true;
	}
}
