/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal;

import tools.refinery.store.map.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Not threadSafe in itself
 *
 * @param <K>
 * @param <V>
 * @author Oszkar Semerath
 */
public class VersionedMapImpl<K, V> implements VersionedMap<K, V> {
	protected final VersionedMapStoreImpl<K, V> store;

	protected final ContinousHashProvider<K> hashProvider;

	protected final V defaultValue;
	protected Node<K, V> root;

	private final OldValueBox<V> oldValueBox = new OldValueBox<>();

	public VersionedMapImpl(
			VersionedMapStoreImpl<K, V> store,
			ContinousHashProvider<K> hashProvider,
			V defaultValue) {
		this.store = store;
		this.hashProvider = hashProvider;
		this.defaultValue = defaultValue;
		this.root = null;
	}

	public VersionedMapImpl(
			VersionedMapStoreImpl<K, V> store,
			ContinousHashProvider<K> hashProvider,
			V defaultValue, Node<K, V> data) {
		this.store = store;
		this.hashProvider = hashProvider;
		this.defaultValue = defaultValue;
		this.root = data;
	}

	public V getDefaultValue() {
		return defaultValue;
	}

	public ContinousHashProvider<K> getHashProvider() {
		return hashProvider;
	}

	@Override
	public V put(K key, V value) {
		if (root != null) {
			root = root.putValue(key, value, oldValueBox, hashProvider, defaultValue, hashProvider.getHash(key, 0), 0);
			return oldValueBox.getOldValue();
		} else {
			root = MutableNode.initialize(key, value, hashProvider, defaultValue);
			return defaultValue;
		}
	}

	@Override
	public void putAll(Cursor<K, V> cursor) {
		if (cursor.getDependingMaps().contains(this)) {
			List<K> keys = new LinkedList<>();
			List<V> values = new LinkedList<>();
			while (cursor.move()) {
				keys.add(cursor.getKey());
				values.add(cursor.getValue());
			}
			Iterator<K> keyIterator = keys.iterator();
			Iterator<V> valueIterator = values.iterator();
			while (keyIterator.hasNext()) {
				this.put(keyIterator.next(), valueIterator.next());
			}
		} else {
			while (cursor.move()) {
				this.put(cursor.getKey(), cursor.getValue());
			}
		}
	}

	@Override
	public V get(K key) {
		if (root != null) {
			return root.getValue(key, hashProvider, defaultValue, hashProvider.getHash(key, 0), 0);
		} else {
			return defaultValue;
		}
	}

	@Override
	public long getSize() {
		if (root == null) {
			return 0;
		} else {
			return root.getSize();
		}
	}

	@Override
	public Cursor<K, V> getAll() {
		return new MapCursor<>(this.root, this);
	}

	@Override
	public DiffCursor<K, V> getDiffCursor(long toVersion) {
		Cursor<K, V> fromCursor = this.getAll();
		VersionedMap<K, V> toMap = this.store.createMap(toVersion);
		Cursor<K, V> toCursor = toMap.getAll();
		return new MapDiffCursor<>(this.hashProvider, this.defaultValue, fromCursor, toCursor);

	}


	@Override
	public long commit() {
		return this.store.commit(root, this);
	}

	public void setRoot(Node<K, V> root) {
		this.root = root;
	}

	@Override
	public void restore(long state) {
		root = this.store.revert(state);
	}

	public void prettyPrint() {
		StringBuilder s = new StringBuilder();
		if (this.root != null) {
			this.root.prettyPrint(s, 0, -1);
			System.out.println(s.toString());
		} else {
			System.out.println("empty tree");
		}
	}

	public void checkIntegrity() {
		if (this.root != null) {
			this.root.checkIntegrity(hashProvider, defaultValue, 0);
		}
	}

	@Override
	public int contentHashCode(ContentHashCode mode) {
		// Calculating the root hashCode is always fast, because {@link Node} caches its hashCode.
		return Objects.hashCode(root);
	}

	@Override
	public boolean contentEquals(AnyVersionedMap other) {
		return other instanceof VersionedMapImpl<?, ?> otherImpl && Objects.equals(root, otherImpl.root);
	}
}
