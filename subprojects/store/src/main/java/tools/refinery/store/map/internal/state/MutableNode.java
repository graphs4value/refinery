/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.state;

import tools.refinery.store.map.ContinuousHashProvider;

import java.util.Arrays;
import java.util.Map;

public class MutableNode<K, V> extends Node<K, V> {
	int cachedHash;
	protected boolean cachedHashValid;
	protected Object[] content;

	protected MutableNode() {
		this.content = new Object[2 * FACTOR];
		invalidateHash();
	}

	public static <K, V> MutableNode<K, V> initialize(K key, V value, ContinuousHashProvider<? super K> hashProvider, V defaultValue) {
		if (value == defaultValue) {
			return null;
		} else {
			int hash = hashProvider.getHash(key, 0);
			int fragment = hashFragment(hash, 0);
			MutableNode<K, V> res = new MutableNode<>();
			res.content[2 * fragment] = key;
			res.content[2 * fragment + 1] = value;
			res.invalidateHash();
			return res;
		}
	}

	/**
	 * Constructs a {@link MutableNode} as a copy of an {@link ImmutableNode}
	 *
	 * @param node to be transformed
	 */
	protected MutableNode(ImmutableNode<K, V> node) {
		this.content = new Object[2 * FACTOR];
		int dataUsed = 0;
		int nodeUsed = 0;
		for (int i = 0; i < FACTOR; i++) {
			int bitPosition = 1 << i;
			if ((node.dataMap & bitPosition) != 0) {
				content[2 * i] = node.content[dataUsed * 2];
				content[2 * i + 1] = node.content[dataUsed * 2 + 1];
				dataUsed++;
			} else if ((node.nodeMap & bitPosition) != 0) {
				content[2 * i + 1] = node.content[node.content.length - 1 - nodeUsed];
				nodeUsed++;
			}
		}
		this.cachedHashValid = false;
	}

	@Override
	public V getValue(K key, ContinuousHashProvider<? super K> hashProvider, V defaultValue, int hash, int depth) {
		int selectedHashFragment = hashFragment(hash, shiftDepth(depth));
		@SuppressWarnings("unchecked") K keyCandidate = (K) this.content[2 * selectedHashFragment];
		if (keyCandidate != null) {
			if (keyCandidate.equals(key)) {
				@SuppressWarnings("unchecked") V value = (V) this.content[2 * selectedHashFragment + 1];
				return value;
			} else {
				return defaultValue;
			}
		} else {
			@SuppressWarnings("unchecked") var nodeCandidate = (Node<K, V>) content[2 * selectedHashFragment + 1];
			if (nodeCandidate != null) {
				int newDepth = incrementDepth(depth);
				int newHash = newHash(hashProvider, key, hash, newDepth);
				return nodeCandidate.getValue(key, hashProvider, defaultValue, newHash, newDepth);
			} else {
				return defaultValue;
			}
		}
	}

	@Override
	public Node<K, V> putValue(K key, V value, OldValueBox<V> oldValueBox, ContinuousHashProvider<? super K> hashProvider, V defaultValue, int hash, int depth) {
		int selectedHashFragment = hashFragment(hash, shiftDepth(depth));
		@SuppressWarnings("unchecked") K keyCandidate = (K) content[2 * selectedHashFragment];
		if (keyCandidate != null) {
			// If it has key
			if (keyCandidate.equals(key)) {
				// The key is equals to an existing key -> update entry
				if (value == defaultValue) {
					return removeEntry(selectedHashFragment, oldValueBox);
				} else {
					return updateValue(value, oldValueBox, selectedHashFragment);
				}
			} else {
				// The key is not equivalent to an existing key on the same hash bin
				// -> split entry if it is necessary
				if (value == defaultValue) {
					// Value is default -> do not need to add new node
					oldValueBox.setOldValue(defaultValue);
					return this;
				} else {
					// Value is not default -> Split entry data to a new node
					oldValueBox.setOldValue(defaultValue);
					return moveDownAndSplit(hashProvider, key, value, keyCandidate, hash, depth, selectedHashFragment);
				}
			}
		}
		// If it does not have key, check for value
		@SuppressWarnings("unchecked") var nodeCandidate = (Node<K, V>) content[2 * selectedHashFragment + 1];
		if (nodeCandidate != null) {
			// If it has value, it is a sub-node -> update that
			int newDepth = incrementDepth(depth);
			var newNode = nodeCandidate.putValue(key, value, oldValueBox, hashProvider, defaultValue, newHash(hashProvider, key, hash, newDepth), newDepth);
			return updateWithSubNode(selectedHashFragment, newNode, (value == null && defaultValue == null) || (value != null && value.equals(defaultValue)));
		} else {
			// If it does not have value, put it in the empty place
			if (value == defaultValue) {
				// don't need to add new key-value pair
				oldValueBox.setOldValue(defaultValue);
				return this;
			} else {
				return addEntry(key, value, oldValueBox, selectedHashFragment, defaultValue);
			}
		}
	}

	private Node<K, V> addEntry(K key, V value, OldValueBox<V> oldValueBox, int selectedHashFragment, V defaultValue) {
		content[2 * selectedHashFragment] = key;
		oldValueBox.setOldValue(defaultValue);
		content[2 * selectedHashFragment + 1] = value;
		invalidateHash();
		return this;
	}

	/**
	 * Updates an entry in a selected hash-fragment to a non-default value.
	 *
	 * @param value new value
	 * @param selectedHashFragment position of the value
	 * @return updated node
	 */
	@SuppressWarnings("unchecked")
	Node<K, V> updateValue(V value, OldValueBox<V> oldValue, int selectedHashFragment) {
		oldValue.setOldValue((V) content[2 * selectedHashFragment + 1]);
		content[2 * selectedHashFragment + 1] = value;
		invalidateHash();
		return this;
	}

	/**
	 * Updates an entry in a selected hash-fragment with a subtree.
	 *
	 * @param selectedHashFragment position of the value
	 * @param newNode the subtree
	 * @return updated node
	 */
	Node<K, V> updateWithSubNode(int selectedHashFragment, Node<K, V> newNode, boolean deletionHappened) {
		if (deletionHappened) {
			if (newNode == null) {
				// Check whether this node become empty
				content[2 * selectedHashFragment + 1] = null; // i.e. the new node
				if (hasContent()) {
					invalidateHash();
					return this;
				} else {
					return null;
				}
			} else {
				// check whether newNode is orphan
				MutableNode<K, V> immutableNewNode = newNode.isMutable();
				if (immutableNewNode != null) {
					int orphaned = immutableNewNode.isOrphaned();
					if (orphaned >= 0) {
						// orphan sub-node data is replaced with data
						content[2 * selectedHashFragment] = immutableNewNode.content[orphaned * 2];
						content[2 * selectedHashFragment + 1] = immutableNewNode.content[orphaned * 2 + 1];
						invalidateHash();
						return this;
					}
				}
			}
		}
		// normal behaviour
		content[2 * selectedHashFragment + 1] = newNode;
		invalidateHash();
		return this;
	}

	private boolean hasContent() {
		for (Object element : this.content) {
			if (element != null) return true;
		}
		return false;
	}

	@Override
	protected MutableNode<K, V> isMutable() {
		return this;
	}

	protected int isOrphaned() {
		int dataFound = -2;
		for (int i = 0; i < FACTOR; i++) {
			if (content[i * 2] != null) {
				if (dataFound >= 0) {
					return -1;
				} else {
					dataFound = i;
				}
			} else if (content[i * 2 + 1] != null) {
				return -3;
			}
		}
		return dataFound;
	}

	@SuppressWarnings("unchecked")
	private Node<K, V> moveDownAndSplit(ContinuousHashProvider<? super K> hashProvider, K newKey, V newValue, K previousKey, int hashOfNewKey, int depth, int selectedHashFragmentOfCurrentDepth) {
		V previousValue = (V) content[2 * selectedHashFragmentOfCurrentDepth + 1];

		MutableNode<K, V> newSubNode = newNodeWithTwoEntries(hashProvider, previousKey, previousValue, hashProvider.getHash(previousKey, hashDepth(depth)), newKey, newValue, hashOfNewKey, incrementDepth(depth));

		content[2 * selectedHashFragmentOfCurrentDepth] = null;
		content[2 * selectedHashFragmentOfCurrentDepth + 1] = newSubNode;
		invalidateHash();
		return this;
	}

	// Pass everything as parameters for performance.
	@SuppressWarnings("squid:S107")
	private MutableNode<K, V> newNodeWithTwoEntries(ContinuousHashProvider<? super K> hashProvider, K key1, V value1, int oldHash1, K key2, V value2, int oldHash2, int newDepth) {
		int newHash1 = newHash(hashProvider, key1, oldHash1, newDepth);
		int newHash2 = newHash(hashProvider, key2, oldHash2, newDepth);
		int newFragment1 = hashFragment(newHash1, shiftDepth(newDepth));
		int newFragment2 = hashFragment(newHash2, shiftDepth(newDepth));

		MutableNode<K, V> subNode = new MutableNode<>();
		if (newFragment1 != newFragment2) {
			subNode.content[newFragment1 * 2] = key1;
			subNode.content[newFragment1 * 2 + 1] = value1;

			subNode.content[newFragment2 * 2] = key2;
			subNode.content[newFragment2 * 2 + 1] = value2;
		} else {
			MutableNode<K, V> subSubNode = newNodeWithTwoEntries(hashProvider, key1, value1, newHash1, key2, value2, newHash2, incrementDepth(newDepth));
			subNode.content[newFragment1 * 2 + 1] = subSubNode;
		}
		subNode.invalidateHash();
		return subNode;
	}

	@SuppressWarnings("unchecked")
	Node<K, V> removeEntry(int selectedHashFragment, OldValueBox<V> oldValue) {
		content[2 * selectedHashFragment] = null;
		oldValue.setOldValue((V) content[2 * selectedHashFragment + 1]);
		content[2 * selectedHashFragment + 1] = null;
		if (hasContent()) {
			invalidateHash();
			return this;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public long getSize() {
		int size = 0;
		for (int i = 0; i < FACTOR; i++) {
			if (content[i * 2] != null) {
				size++;
			} else {
				Node<K, V> nodeCandidate = (Node<K, V>) content[i * 2 + 1];
				if (nodeCandidate != null) {
					size += nodeCandidate.getSize();
				}
			}
		}
		return size;
	}

	@Override
	protected MutableNode<K, V> toMutable() {
		return this;
	}

	@Override
	public ImmutableNode<K, V> toImmutable(Map<Node<K, V>, ImmutableNode<K, V>> cache) {
		return ImmutableNode.constructImmutable(this, cache);
	}

	@SuppressWarnings("unchecked")
	@Override
	boolean moveToNext(MapCursor<K, V> cursor) {
		// 1. try to move to data
		if (cursor.dataIndex != MapCursor.INDEX_FINISH) {
			for (int index = cursor.dataIndex + 1; index < FACTOR; index++) {
				if (this.content[index * 2] != null) {
					// 1.1 found next data
					cursor.dataIndex = index;
					cursor.key = (K) this.content[index * 2];
					cursor.value = (V) this.content[index * 2 + 1];
					return true;
				}
			}
			cursor.dataIndex = MapCursor.INDEX_FINISH;
		}

		// 2. look inside the sub-nodes
		if(cursor.nodeIndexStack.peek()==null) {
			throw new IllegalStateException("Cursor moved to the next state when the state is empty.");
		}
		for (int index = cursor.nodeIndexStack.peek() + 1; index < FACTOR; index++) {
			if (this.content[index * 2] == null && this.content[index * 2 + 1] != null) {
				// 2.1 found next sub-node, move down to the sub-node
				Node<K, V> subnode = (Node<K, V>) this.content[index * 2 + 1];

				cursor.dataIndex = MapCursor.INDEX_START;
				cursor.nodeIndexStack.pop();
				cursor.nodeIndexStack.push(index);
				cursor.nodeIndexStack.push(MapCursor.INDEX_START);
				cursor.nodeStack.push(subnode);

				return subnode.moveToNext(cursor);
			}
		}
		// 3. no sub-node found, move up
		cursor.nodeStack.pop();
		cursor.nodeIndexStack.pop();
		if (!cursor.nodeStack.isEmpty()) {
			Node<K, V> supernode = cursor.nodeStack.peek();
			return supernode.moveToNext(cursor);
		} else {
			cursor.key = null;
			cursor.value = null;
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	boolean moveToNextInorder(InOrderMapCursor<K,V> cursor) {
		if(cursor.nodeIndexStack.peek()==null || cursor.nodeStack.peek()==null) {
			throw new IllegalStateException("Cursor moved to the next state when the state is empty.");
		}

		int position = cursor.nodeIndexStack.peek();

		for (int index = position + 1; index < FACTOR; index++) {
			// data found
			if (this.content[index * 2] != null) {
				cursor.nodeIndexStack.pop();
				cursor.nodeIndexStack.push(index);

				cursor.key = (K) this.content[index * 2];
				cursor.value = (V) this.content[index * 2 + 1];
				return true;
			} else if (this.content[index * 2 +1] != null) {
				// sub-node found
				Node<K,V> subnode = (Node<K, V>) this.content[index * 2 +1];
				cursor.nodeIndexStack.pop();
				cursor.nodeIndexStack.push(index);
				cursor.nodeIndexStack.push(InOrderMapCursor.INDEX_START);
				cursor.nodeStack.push(subnode);

				return subnode.moveToNextInorder(cursor);
			}
		}

		// nothing found
		cursor.nodeStack.pop();
		cursor.nodeIndexStack.pop();
		if (!cursor.nodeStack.isEmpty()) {
			Node<K, V> supernode = cursor.nodeStack.peek();
			return supernode.moveToNextInorder(cursor);
		} else {
			cursor.key = null;
			cursor.value = null;
			return false;
		}
	}

	@Override
	public void prettyPrint(StringBuilder builder, int depth, int code) {
		builder.append("\t".repeat(Math.max(0, depth)));
		if (code >= 0) {
			builder.append(code);
			builder.append(":");
		}
		builder.append("Mutable(");
		// print content
		boolean hadContent = false;
		for (int i = 0; i < FACTOR; i++) {
			if (content[2 * i] != null) {
				if (hadContent) {
					builder.append(",");
				}
				builder.append(i);
				builder.append(":[");
				builder.append(content[2 * i].toString());
				builder.append("]->[");
				builder.append(content[2 * i + 1].toString());
				builder.append("]");
				hadContent = true;
			}
		}
		builder.append(")");
		// print sub-nodes
		for (int i = 0; i < FACTOR; i++) {
			if (content[2 * i] == null && content[2 * i + 1] != null) {
				@SuppressWarnings("unchecked") Node<K, V> subNode = (Node<K, V>) content[2 * i + 1];
				builder.append("\n");
				subNode.prettyPrint(builder, incrementDepth(depth), i);
			}
		}
	}

	@Override
	public void checkIntegrity(ContinuousHashProvider<? super K> hashProvider, V defaultValue, int depth) {
		// check for orphan nodes
		if (depth > 0) {
			int orphaned = isOrphaned();
			if (orphaned >= 0) {
				throw new IllegalStateException("Orphaned node! " + orphaned + ": " + content[2 * orphaned]);
			}
		}
		// check the place of data
		for (int i = 0; i < FACTOR; i++) {
			if (this.content[2 * i] != null) {
				@SuppressWarnings("unchecked") K key = (K) this.content[2 * i];
				@SuppressWarnings("unchecked") V value = (V) this.content[2 * i + 1];

				if (value == defaultValue) {
					throw new IllegalStateException("Node contains default value!");
				}
				int hashCode = hashProvider.getHash(key, hashDepth(depth));
				int shiftDepth = shiftDepth(depth);
				int selectedHashFragment = hashFragment(hashCode, shiftDepth);
				if (i != selectedHashFragment) {
					throw new IllegalStateException("Key " + key + " with hash code " + hashCode + " is in bad place! Fragment=" + selectedHashFragment + ", Place=" + i);
				}
			}
		}
		// check sub-nodes
		for (int i = 0; i < FACTOR; i++) {
			if (this.content[2 * i + 1] != null && this.content[2 * i] == null) {
				@SuppressWarnings("unchecked") var subNode = (Node<K, V>) this.content[2 * i + 1];
				subNode.checkIntegrity(hashProvider, defaultValue, incrementDepth(depth));
			}
		}
		// check the hash
		if (cachedHashValid) {
			int oldHash = this.cachedHash;
			invalidateHash();
			int newHash = hashCode();
			if (oldHash != newHash) {
				throw new IllegalStateException("Hash code was not up to date! (old=" + oldHash + ",new=" + newHash + ")");
			}
		}
	}

	protected void invalidateHash() {
		this.cachedHashValid = false;
	}

	@Override
	public int hashCode() {
		if (!this.cachedHashValid) {
			this.cachedHash = Arrays.hashCode(content);
			this.cachedHashValid = true;
		}
		return this.cachedHash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof MutableNode<?, ?> mutableObj) {
			if (obj.hashCode() != this.hashCode()) {
				return false;
			} else {
				for (int i = 0; i < FACTOR * 2; i++) {
					Object thisContent = this.content[i];
					if (thisContent != null && !thisContent.equals(mutableObj.content[i])) {
						return false;
					}
				}
				return true;
			}
		} else if (obj instanceof ImmutableNode<?, ?> immutableObj) {
			return ImmutableNode.compareImmutableMutable(immutableObj, this);
		} else {
			return false;
		}
	}
}
