/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.state;

import tools.refinery.store.map.AnyVersionedMap;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A cursor representing the difference between two states of a map.
 *
 * @author Oszkar Semerath
 */
public class MapDiffCursor<K, V> implements DiffCursor<K, V>, Cursor<K, V> {
	private enum State {
		/**
		 * initialized state.
		 */
		INIT,
		/**
		 * Unstable state.
		 */
		MOVING_MOVING_SAME_KEY_SAME_VALUE,
		/**
		 * Both cursors are moving, and they are on the same sub-node.
		 */
		MOVING_MOVING_SAME_NODE,
		/**
		 * Both cursors are moving, cursor 1 is behind.
		 */
		MOVING_MOVING_BEHIND1,
		/**
		 * Both cursors are moving, cursor 2 is behind.
		 */
		MOVING_MOVING_BEHIND2,
		/**
		 * Both cursors are moving, cursor 1 is on the same key as cursor 2, values are different
		 */
		MOVING_MOVING_SAME_KEY_DIFFERENT_VALUE,
		/**
		 * Cursor 1 is moving, Cursor 2 is terminated.
		 */
		MOVING_TERMINATED,
		/**
		 * Cursor 1 is terminated , Cursor 2 is moving.
		 */
		TERMINATED_MOVING,
		/**
		 * Both cursors are terminated.
		 */
		TERMINATED_TERMINATED,
		/**
		 * Both Cursors are moving, and they are on an incomparable position.
		 * It is resolved by showing Cursor 1.
		 */
		MOVING_MOVING_HASH1,
		/**
		 * Both Cursors are moving, and they are on an incomparable position.
		 * It is resolved by showing Cursor 2.
		 */
		MOVING_MOVING_HASH2
	}

	/**
	 * Default nodeId representing missing elements.
	 */
	private final V defaultValue;
	private final InOrderMapCursor<K, V> cursor1;
	private final InOrderMapCursor<K, V> cursor2;

	// State
	State state = State.INIT;

	// Values
	private K key;
	private V fromValue;
	private V toValue;


	public MapDiffCursor(V defaultValue, InOrderMapCursor<K, V> cursor1, InOrderMapCursor<K, V> cursor2) {
		super();
		this.defaultValue = defaultValue;
		this.cursor1 = cursor1;
		this.cursor2 = cursor2;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getFromValue() {
		return fromValue;
	}

	@Override
	public V getToValue() {
		return toValue;
	}

	@Override
	public V getValue() {
		return getToValue();
	}

	public boolean isTerminated() {
		return this.state == State.TERMINATED_TERMINATED;
	}

	@Override
	public boolean isDirty() {
		return this.cursor1.isDirty() || this.cursor2.isDirty();
	}

	@Override
	public Set<AnyVersionedMap> getDependingMaps() {
		return Stream.concat(cursor1.getDependingMaps().stream(), cursor2.getDependingMaps().stream()).map(AnyVersionedMap.class::cast).collect(Collectors.toUnmodifiableSet());
	}

	private boolean isInStableState() {
		return this.state != State.MOVING_MOVING_SAME_KEY_SAME_VALUE
				&& this.state != State.MOVING_MOVING_SAME_NODE && this.state != State.INIT;
	}

	private boolean updateAndReturnWithResult() {
		return switch (this.state) {
			case INIT -> throw new IllegalStateException("DiffCursor terminated without starting!");
			case MOVING_MOVING_SAME_KEY_SAME_VALUE, MOVING_MOVING_SAME_NODE ->
					throw new IllegalStateException("DiffCursor terminated in unstable state!");
			case MOVING_MOVING_BEHIND1, MOVING_TERMINATED, MOVING_MOVING_HASH1 -> {
				this.key = this.cursor1.getKey();
				this.fromValue = this.cursor1.getValue();
				this.toValue = this.defaultValue;
				yield true;
			}
			case MOVING_MOVING_BEHIND2, TERMINATED_MOVING, MOVING_MOVING_HASH2 -> {
				this.key = this.cursor2.getKey();
				this.fromValue = this.defaultValue;
				this.toValue = cursor2.getValue();
				yield true;
			}
			case MOVING_MOVING_SAME_KEY_DIFFERENT_VALUE -> {
				this.key = this.cursor1.getKey();
				this.fromValue = this.cursor1.getValue();
				this.toValue = this.cursor2.getValue();
				yield true;
			}
			case TERMINATED_TERMINATED -> {
				this.key = null;
				this.fromValue = null;
				this.toValue = null;
				yield false;
			}
		};
	}

	public boolean move() {
		do {
			this.state = moveOne(this.state);
		} while (!isInStableState());
		return updateAndReturnWithResult();
	}

	private State moveOne(State currentState) {
		return switch (currentState) {
			case INIT, MOVING_MOVING_HASH2, MOVING_MOVING_SAME_KEY_SAME_VALUE, MOVING_MOVING_SAME_KEY_DIFFERENT_VALUE -> {
				boolean cursor1Moved = cursor1.move();
				boolean cursor2Moved = cursor2.move();
				yield recalculateStateAfterCursorMovement(cursor1Moved, cursor2Moved);
			}
			case MOVING_MOVING_SAME_NODE -> {
				boolean cursor1Moved = cursor1.skipCurrentNode();
				boolean cursor2Moved = cursor2.skipCurrentNode();
				yield recalculateStateAfterCursorMovement(cursor1Moved, cursor2Moved);
			}
			case MOVING_MOVING_BEHIND1 -> {
				boolean cursorMoved = cursor1.move();
				if (cursorMoved) {
					yield recalculateStateBasedOnCursorRelation();
				} else {
					yield State.TERMINATED_MOVING;
				}
			}
			case MOVING_MOVING_BEHIND2 -> {
				boolean cursorMoved = cursor2.move();
				if (cursorMoved) {
					yield recalculateStateBasedOnCursorRelation();
				} else {
					yield State.MOVING_TERMINATED;
				}
			}
			case TERMINATED_MOVING -> {
				boolean cursorMoved = cursor2.move();
				if (cursorMoved) {
					yield State.TERMINATED_MOVING;
				} else {
					yield State.TERMINATED_TERMINATED;
				}
			}
			case MOVING_TERMINATED -> {
				boolean cursorMoved = cursor1.move();
				if (cursorMoved) {
					yield State.MOVING_TERMINATED;
				} else {
					yield State.TERMINATED_TERMINATED;
				}
			}
			case MOVING_MOVING_HASH1 -> State.MOVING_MOVING_HASH2;
			case TERMINATED_TERMINATED -> throw new IllegalStateException("Trying to move while terminated!");
		};
	}

	private State recalculateStateAfterCursorMovement(boolean cursor1Moved, boolean cursor2Moved) {
		if (cursor1Moved && cursor2Moved) {
			return recalculateStateBasedOnCursorRelation();
		} else if (cursor1Moved) {
			return State.MOVING_TERMINATED;
		} else if (cursor2Moved) {
			return State.TERMINATED_MOVING;
		} else {
			return State.TERMINATED_TERMINATED;
		}
	}

	private State recalculateStateBasedOnCursorRelation() {
		final int relation = InOrderMapCursor.comparePosition(cursor1, cursor2);

		if (relation > 0) {
			return State.MOVING_MOVING_BEHIND1;
		} else if (relation < 0) {
			return State.MOVING_MOVING_BEHIND2;
		}

		if (InOrderMapCursor.sameSubNode(cursor1, cursor2)) {
			return State.MOVING_MOVING_SAME_NODE;
		} else if (Objects.equals(cursor1.getKey(), cursor2.getKey())) {
			if (Objects.equals(cursor1.getValue(), cursor2.getValue())) {
				return State.MOVING_MOVING_SAME_KEY_SAME_VALUE;
			} else {
				return State.MOVING_MOVING_SAME_KEY_DIFFERENT_VALUE;
			}
		}

		final int depth = InOrderMapCursor.compareDepth(cursor1, cursor2);

		if (depth > 0) {
			return State.MOVING_MOVING_BEHIND1;
		} else if (depth < 0) {
			return State.MOVING_MOVING_BEHIND2;
		} else {
			return State.MOVING_MOVING_HASH1;
		}

	}
}
