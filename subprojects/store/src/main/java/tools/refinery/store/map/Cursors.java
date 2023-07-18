/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

import java.util.Iterator;
import java.util.Map;

public final class Cursors {
    private Cursors() {
        throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
    }

    public static <K, V> Cursor<K, V> empty() {
        return new Empty<>();
    }

	public static <K, V> Cursor<K, V> singleton(K key, V value) {
		return new Singleton<>(key, value);
	}

	public static <K, V> Cursor<K, V> of(Iterator<Map.Entry<K, V>> iterator) {
		return new IteratorBasedCursor<>(iterator);
	}

	public static <K, V> Cursor<K, V> of(Map<K, V> map) {
		return of(map.entrySet().iterator());
	}

    private static class Empty<K, V> implements Cursor<K, V> {
        private boolean terminated = false;

        @Override
        public K getKey() {
            return null;
        }

        @Override
        public V getValue() {
            return null;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }

        @Override
        public boolean move() {
            terminated = true;
            return false;
        }
    }

	private static class Singleton<K, V> implements Cursor<K, V> {
		private State state = State.INITIAL;
		private final K key;
		private final V value;

		public Singleton(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			if (state == State.STARTED) {
				return key;
			}
			return null;
		}

		@Override
		public V getValue() {
			if (state == State.STARTED) {
				return value;
			}
			return null;
		}

		@Override
		public boolean isTerminated() {
			return state == State.TERMINATED;
		}

		@Override
		public boolean move() {
			if (state == State.INITIAL) {
				state = State.STARTED;
				return true;
			}
			state = State.TERMINATED;
			return false;
		}


		private enum State {
			INITIAL,
			STARTED,
			TERMINATED
		}
	}
}
