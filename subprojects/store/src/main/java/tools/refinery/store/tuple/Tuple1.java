/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.tuple;

import org.jetbrains.annotations.NotNull;
import tools.refinery.store.model.TupleHashProvider;

import java.util.Arrays;

import static tools.refinery.store.tuple.TupleConstants.TUPLE_BEGIN;
import static tools.refinery.store.tuple.TupleConstants.TUPLE_END;

public final class Tuple1 implements Tuple {
	private final int value0;

	private Tuple1(int value0) {
		this.value0 = value0;
	}

	public int value0() {
		return value0;
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public int get(int element) {
		if (element == 0) {
			return value0;
		}
		throw new IndexOutOfBoundsException(element);
	}

	@Override
	public Tuple set(int element, int value) {
		if (element == 0) {
			return Tuple.of(value);
		}
		throw new IndexOutOfBoundsException(element);
	}

	@Override
	public String toString() {
		return TUPLE_BEGIN + value0 + TUPLE_END;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tuple1 tuple1 = (Tuple1) o;
		return value0 == tuple1.value0;
	}

	@Override
	public int hashCode() {
		return 31 + value0;
	}

	@Override
	public int compareTo(@NotNull Tuple other) {
		if (other instanceof Tuple1 other1) {
			return Integer.compare(value0, other1.value0);
		}
		return Tuple.super.compareTo(other);
	}

	/**
	 * This class uses safe double-checked locking, see
	 * <a href="https://shipilev.net/blog/2014/safe-public-construction/">Safe Publication and Safe Initialization in
	 * Java</a> for details.
	 * <p>
	 * This class implements the singleton pattern to ensure only a single cache exists. This is thread-safe because
	 * of the locking of the cache.
	 */
	@SuppressWarnings("squid:S6548")
	public static class Cache {
		private static final int MIN_CACHE_SIZE = 256;

		private static final int MAX_CACHE_SIZE = TupleHashProvider.MAX_MODEL_SIZE;

		public static final Cache INSTANCE = new Cache();

		private final Object lock = new Object();

		// We don't want to synchronize the elements of the array, just the array reference itself, so an
		// AtomicReferenceArray is not needed here and would degrade performance.
		@SuppressWarnings("squid:S3077")
		private volatile Tuple1[] tuple1Cache;

		private Cache() {
			reset();
		}

		public void reset() {
			synchronized (lock) {
				var newCache = new Tuple1[MIN_CACHE_SIZE];
				for (int i = 0; i < newCache.length; i++) {
					newCache[i] = new Tuple1(i);
				}
				tuple1Cache = newCache;
			}
		}

		public Tuple1 getOrCreate(int value) {
			if (value < 0 || value >= MAX_CACHE_SIZE) {
				return new Tuple1(value);
			}
			var currentCache = tuple1Cache;
			if (value < currentCache.length) {
				return currentCache[value];
			}
			synchronized (lock) {
				currentCache = tuple1Cache;
				int currentSize = currentCache.length;
				if (value < currentSize) {
					return currentCache[value];
				}
				// We don't have to worry about currentSize + (currentSize >> 1) overflowing, because MAX_CACHE_SIZE
				// is only 30 bits.
				int newSize = Math.min(Math.max(value + 1, currentSize + (currentSize >> 1)), MAX_CACHE_SIZE);
				var newCache = Arrays.copyOf(currentCache, newSize);
				for (int i = currentSize; i < newSize; i++) {
					newCache[i] = new Tuple1(i);
				}
				tuple1Cache = newCache;
				return newCache[value];
			}
		}
	}
}
