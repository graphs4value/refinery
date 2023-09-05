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

import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.Set;

public class MapCursor<K, V> implements Cursor<K, V> {
	// Constants
	static final int INDEX_START = -1;
	static final int INDEX_FINISH = -2;

	// Tree stack
	ArrayDeque<Node<K, V>> nodeStack;
	ArrayDeque<Integer> nodeIndexStack;
	int dataIndex;

	// Values
	K key;
	V value;

	// Hash code for checking concurrent modifications
	final VersionedMap<K, V> map;
	final int creationHash;

	public MapCursor(Node<K, V> root, VersionedMap<K, V> map) {
		// Initializing tree stack
		super();
		this.nodeStack = new ArrayDeque<>();
		this.nodeIndexStack = new ArrayDeque<>();
		if (root != null) {
			this.nodeStack.add(root);
			this.nodeIndexStack.push(INDEX_START);
		}

		this.dataIndex = INDEX_START;

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
			boolean result = node.moveToNext(this);
			if (this.nodeIndexStack.size() != this.nodeStack.size()) {
				throw new IllegalArgumentException("Node stack is corrupted by illegal moves!");
			}
			return result;
		}
		return false;
	}

	@Override
	public boolean isDirty() {
		return this.map.contentHashCode(ContentHashCode.APPROXIMATE_FAST) != this.creationHash;
	}

	@Override
	public Set<AnyVersionedMap> getDependingMaps() {
		return Set.of(this.map);
	}
}
