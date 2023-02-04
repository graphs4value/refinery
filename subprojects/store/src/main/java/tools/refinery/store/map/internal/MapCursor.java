package tools.refinery.store.map.internal;

import tools.refinery.store.map.AnyVersionedMap;
import tools.refinery.store.map.ContentHashCode;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.VersionedMap;

import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
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

	public boolean skipCurrentNode() {
		nodeStack.pop();
		nodeIndexStack.pop();
		dataIndex = INDEX_FINISH;
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

	public static <K, V> boolean sameSubNode(MapCursor<K, V> cursor1, MapCursor<K, V> cursor2) {
		Node<K, V> nodeOfCursor1 = cursor1.nodeStack.peek();
		Node<K, V> nodeOfCursor2 = cursor2.nodeStack.peek();
		if (nodeOfCursor1 != null && nodeOfCursor2 != null) {
			return nodeOfCursor1.equals(nodeOfCursor2);
		} else {
			return false;
		}
	}

	/**
	 * Compares the state of two cursors started on two @{@link VersionedMap of the }same
	 * {@link tools.refinery.store.map.VersionedMapStore}.
	 * @param <K> Key type
	 * @param <V> Value type
	 * @param cursor1 first cursor
	 * @param cursor2 second cursor
	 * @return Positive number if cursor 1 is behind, negative number if cursor 2 is behind, and 0 if they are at the
	 * same position.
	 */
	public static <K, V> int compare(MapCursor<K, V> cursor1, MapCursor<K, V> cursor2) {
		// two cursors are equally deep
		Iterator<Integer> stack1 = cursor1.nodeIndexStack.descendingIterator();
		Iterator<Integer> stack2 = cursor2.nodeIndexStack.descendingIterator();
		if (stack1.hasNext()) {
			if (!stack2.hasNext()) {
				// stack 2 has no more element, thus stack 1 is deeper
				return 1;
			}
			int val1 = stack1.next();
			int val2 = stack2.next();
			if (val1 < val2) {
				return -1;
			} else if (val2 < val1) {
				return 1;
			}
		}
		if (stack2.hasNext()) {
			// stack 2 has more element, thus stack 2 is deeper
			return 1;
		}
		return Integer.compare(cursor1.dataIndex, cursor2.dataIndex);
	}
}
