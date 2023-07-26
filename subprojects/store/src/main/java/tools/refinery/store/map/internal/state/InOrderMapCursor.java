/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.state;

import tools.refinery.store.map.AnyVersionedMap;
import tools.refinery.store.map.ContentHashCode;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.VersionedMap;

import java.util.*;

public class InOrderMapCursor<K, V> implements Cursor<K, V> {
	// Constants
	static final int INDEX_START = -1;

	// Tree stack
	ArrayDeque<Node<K, V>> nodeStack;
	ArrayDeque<Integer> nodeIndexStack;


	// Values
	K key;
	V value;

	// Hash code for checking concurrent modifications
	final VersionedMap<K, V> map;
	final int creationHash;

	public InOrderMapCursor(VersionedMapStateImpl<K, V> map) {
		// Initializing tree stack
		super();
		this.nodeStack = new ArrayDeque<>();
		this.nodeIndexStack = new ArrayDeque<>();
		if (map.root != null) {
			this.nodeStack.add(map.root);
			this.nodeIndexStack.push(INDEX_START);
		}

		// Initializing cache
		this.key = null;
		this.value = null;

		// Initializing state
		this.map = map;
		this.creationHash = map.contentHashCode(ContentHashCode.APPROXIMATE_FAST);
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public boolean isTerminated() {
		return this.nodeStack.isEmpty();
	}

	public boolean move() {
		if (isDirty()) {
			throw new ConcurrentModificationException();
		}
		if (!isTerminated()) {
			var node = this.nodeStack.peek();
			if (node == null) {
				throw new IllegalStateException("Cursor is not terminated but the current node is missing");
			}
			boolean result = node.moveToNextInorder(this);
			if (this.nodeIndexStack.size() != this.nodeStack.size()) {
				throw new IllegalArgumentException("Node stack is corrupted by illegal moves!");
			}
			return result;
		}
		return false;
	}

	public boolean skipCurrentNode() {
		nodeStack.pop();
		nodeIndexStack.pop();
		return move();
	}

	@Override
	public boolean isDirty() {
		return this.map.contentHashCode(ContentHashCode.APPROXIMATE_FAST) != this.creationHash;
	}

	@Override
	public Set<AnyVersionedMap> getDependingMaps() {
		return Set.of(this.map);
	}

	public static <K, V> boolean sameSubNode(InOrderMapCursor<K, V> cursor1, InOrderMapCursor<K, V> cursor2) {
		Node<K, V> nodeOfCursor1 = cursor1.nodeStack.peek();
		Node<K, V> nodeOfCursor2 = cursor2.nodeStack.peek();
		return Objects.equals(nodeOfCursor1, nodeOfCursor2);
	}

	/**
	 * Compares the state of two cursors started on two {@link VersionedMap} of the same
	 * {@link tools.refinery.store.map.VersionedMapStore}.
	 * @param <K> Key type
	 * @param <V> Value type
	 * @param cursor1 first cursor
	 * @param cursor2 second cursor
	 * @return Positive number if cursor 1 is behind, negative number if cursor 2 is behind, and 0 if they are at the
	 * same position.
	 */
	public static <K, V> int comparePosition(InOrderMapCursor<K, V> cursor1, InOrderMapCursor<K, V> cursor2) {
		// If the state does not determine the order, then compare @nodeIndexStack.
		Iterator<Integer> nodeIndexStack1 = cursor1.nodeIndexStack.descendingIterator();
		Iterator<Integer> nodeIndexStack2 = cursor2.nodeIndexStack.descendingIterator();

		while(nodeIndexStack1.hasNext() && nodeIndexStack2.hasNext()){
			final int index1 = nodeIndexStack1.next();
			final int index2 = nodeIndexStack2.next();
			if(index1 < index2) {
				return 1;
			} else if(index1 > index2) {
				return -1;
			}
		}

		return 0;
	}

	/**
	 * Compares the depth of two cursors started on  @{@link VersionedMap} of the same
	 * 	  {@link tools.refinery.store.map.VersionedMapStore}.
	 * @param <K> Key type
	 * @param <V> Value type
	 * @param cursor1 first cursor
	 * @param cursor2 second cursor
	 * @return Positive number if cursor 1 is deeper, negative number if cursor 2 is deeper, and 0 if they are at the
	 *  same depth.
	 */
	public static <K, V> int compareDepth(InOrderMapCursor<K, V> cursor1, InOrderMapCursor<K, V> cursor2) {
		int d1 = cursor1.nodeIndexStack.size();
		int d2 = cursor2.nodeIndexStack.size();
		return Integer.compare(d1, d2);
	}
}
