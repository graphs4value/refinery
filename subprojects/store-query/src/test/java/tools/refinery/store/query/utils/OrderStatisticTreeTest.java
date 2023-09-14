/*
 * Copyright (c) 2021 Rodion Efremov
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT
 */
package tools.refinery.store.query.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for an order statistic tree which is based on AVL-trees.
 * <p>
 * This class was copied into <i>Refinery</i> from
 * <a href="https://github.com/coderodde/OrderStatisticTree/tree/546c343b9f5d868e394a079ff32691c9dbfd83e3">https://github.com/coderodde/OrderStatisticTree</a>
 * and is available under the
 * <a href="https://github.com/coderodde/OrderStatisticTree/blob/master/LICENSE">MIT License</a>.
 * We also migrated the code to Junit 5, cleaned up some linter warnings, and made the tests deterministic by fixing
 * the random seeds.
 *
 * @author Rodion "rodde" Efremov
 * @version based on 1.6 (Feb 11, 2016)
 */
class OrderStatisticTreeTest {
	private final OrderStatisticTree<Integer> tree = new OrderStatisticTree<>();

	private final TreeSet<Integer> set = new TreeSet<>();

	@BeforeEach
	void before() {
		tree.clear();
		set.clear();
	}

	@Test
	void testAdd() {
		assertEquals(set.isEmpty(), tree.isEmpty());

		for (int i = 10; i < 30; i += 2) {
			assertTrue(tree.isHealthy());
			assertEquals(set.contains(i), tree.contains(i));
			assertEquals(set.add(i), tree.add(i));
			assertEquals(set.add(i), tree.add(i));
			assertEquals(set.contains(i), tree.contains(i));
			assertTrue(tree.isHealthy());
		}

		assertEquals(set.isEmpty(), tree.isEmpty());
	}

	@Test
	void testAddAll() {
		for (int i = 0; i < 10; ++i) {
			assertEquals(set.add(i), tree.add(i));
		}

		Collection<Integer> coll = Arrays.asList(10, 9, 7, 11, 12);

		assertEquals(set.addAll(coll), tree.addAll(coll));
		assertEquals(set.size(), tree.size());

		for (int i = -10; i < 20; ++i) {
			assertEquals(set.contains(i), tree.contains(i));
		}
	}

	@Test
	void testClear() {
		for (int i = 0; i < 2000; ++i) {
			set.add(i);
			tree.add(i);
		}

		assertEquals(set.size(), tree.size());
		set.clear();
		tree.clear();
		// We expect {@code tree.size()} to always be 0, but we also test for it.
		//noinspection ConstantValue
		assertEquals(0, tree.size());
	}

	@Test
	void testContains() {
		for (int i = 100; i < 200; i += 3) {
			assertTrue(tree.isHealthy());
			assertEquals(set.add(i), tree.add(i));
			assertTrue(tree.isHealthy());
		}

		assertEquals(set.size(), tree.size());

		for (int i = 0; i < 300; ++i) {
			assertEquals(set.contains(i), tree.contains(i));
		}
	}

	@Test
	void testContainsAll() {
		for (int i = 0; i < 50; ++i) {
			set.add(i);
			tree.add(i);
		}

		Collection<Integer> coll = new HashSet<>();

		for (int i = 10; i < 20; ++i) {
			coll.add(i);
		}

		assertEquals(set.containsAll(coll), tree.containsAll(coll));
		coll.add(100);
		assertEquals(set.containsAll(coll), tree.containsAll(coll));
	}

	@Test
	void testRemove() {
		for (int i = 0; i < 200; ++i) {
			assertEquals(set.add(i), tree.add(i));
		}

		for (int i = 50; i < 150; i += 2) {
			assertEquals(set.remove(i), tree.remove(i));
			assertTrue(tree.isHealthy());
		}

		for (int i = -100; i < 300; ++i) {
			assertEquals(set.contains(i), tree.contains(i));
		}
	}

	@Test
	void testRemoveLast() {
		tree.add(1);
		tree.remove(1);
		assertEquals(0, tree.size());
	}

	@Test
	void testRemoveAll() {
		for (int i = 0; i < 40; ++i) {
			set.add(i);
			tree.add(i);
		}

		Collection<Integer> coll = new HashSet<>();

		for (int i = 10; i < 20; ++i) {
			coll.add(i);
		}

		assertEquals(set.removeAll(coll), tree.removeAll(coll));

		for (int i = -10; i < 50; ++i) {
			assertEquals(set.contains(i), tree.contains(i));
		}

		assertEquals(set.removeAll(coll), tree.removeAll(coll));

		for (int i = -10; i < 50; ++i) {
			assertEquals(set.contains(i), tree.contains(i));
		}
	}

	@Test
	void testSize() {
		for (int i = 0; i < 200; ++i) {
			assertEquals(set.size(), tree.size());
			assertEquals(set.add(i), tree.add(i));
			assertEquals(set.size(), tree.size());
		}
	}

	@Test
	void testIndexOf() {
		for (int i = 0; i < 100; ++i) {
			assertTrue(tree.add(i * 2));
		}

		for (int i = 0; i < 100; ++i) {
			assertEquals(i, tree.indexOf(2 * i));
		}

		for (int i = 100; i < 150; ++i) {
			assertEquals(-1, tree.indexOf(2 * i));
		}
	}

	@Test
	void testEmpty() {
		assertEquals(set.isEmpty(), tree.isEmpty());
		set.add(0);
		tree.add(0);
		assertEquals(set.isEmpty(), tree.isEmpty());
	}

	@Test
	void testEmptyTreeGetThrowsOnNegativeIndex() {
		assertThrows(IndexOutOfBoundsException.class, () -> tree.get(-1));
	}

	@Test
	void testEmptyTreeSelectThrowsOnTooLargeIndex() {
		assertThrows(IndexOutOfBoundsException.class, () -> tree.get(0));
	}

	@Test
	void testSelectThrowsOnNegativeIndex() {
		for (int i = 0; i < 5; ++i) {
			tree.add(i);
		}

		assertThrows(IndexOutOfBoundsException.class, () -> tree.get(-1));
	}

	@Test
	void testSelectThrowsOnTooLargeIndex() {
		for (int i = 0; i < 5; ++i) {
			tree.add(i);
		}

		assertThrows(IndexOutOfBoundsException.class, () -> tree.get(5));
	}

	@Test
	void testGet() {
		for (int i = 0; i < 100; i += 3) {
			tree.add(i);
		}

		for (int i = 0; i < tree.size(); ++i) {
			assertEquals(Integer.valueOf(3 * i), tree.get(i));
		}
	}

	@Test
	void findBug() {
		tree.add(0);
		assertTrue(tree.isHealthy());

		tree.add(-1);
		tree.remove(-1);
		assertTrue(tree.isHealthy());

		tree.add(1);
		tree.remove(1);
		assertTrue(tree.isHealthy());

		tree.add(-1);
		tree.add(1);
		tree.remove(0);
		assertTrue(tree.isHealthy());

		tree.clear();
		tree.add(0);
		tree.add(-1);
		tree.add(10);
		tree.add(5);
		tree.add(15);
		tree.add(11);
		tree.add(30);
		tree.add(7);

		tree.remove(-1);

		assertTrue(tree.isHealthy());
	}

	@ParameterizedTest(name = "seed = {0}")
	@MethodSource("seedSource")
	void tryReproduceTheCounterBug(long seed) {
		Random random = new Random(seed);
		List<Integer> list = new ArrayList<>();

		for (int i = 0; i < 10; ++i) {
			int number = random.nextInt(1000);
			list.add(number);
			tree.add(number);
			assertTrue(tree.isHealthy());
		}

		for (Integer i : list) {
			tree.remove(i);
			boolean healthy = tree.isHealthy();
			assertTrue(healthy);
		}
	}

	@Test
	void testEmptyIterator() {
		var iterator = tree.iterator();
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	void testIteratorThrowsOnDoubleRemove() {
		for (int i = 10; i < 20; ++i) {
			set.add(i);
			tree.add(i);
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		for (int i = 0; i < 3; ++i) {
			assertEquals(iterator1.next(), iterator2.next());
		}

		iterator1.remove();
		iterator2.remove();

		assertThrows(IllegalStateException.class, iterator1::remove);
		assertThrows(IllegalStateException.class, iterator2::remove);
	}

	@Test
	void testIterator() {
		for (int i = 0; i < 5; ++i) {
			tree.add(i);
			set.add(i);
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		for (int i = 0; i < 5; ++i) {
			assertEquals(iterator1.hasNext(), iterator2.hasNext());
			assertEquals(iterator1.next(), iterator2.next());
		}

		assertEquals(iterator1.hasNext(), iterator2.hasNext());

		assertThrows(NoSuchElementException.class, iterator1::next);
		assertThrows(NoSuchElementException.class, iterator2::next);
	}

	@Test
	void testRemoveBeforeNextThrowsEmpty() {
		var setIterator = set.iterator();
		assertThrows(IllegalStateException.class, setIterator::remove);

		var treeIterator = tree.iterator();
		assertThrows(IllegalStateException.class, treeIterator::remove);
	}

	@Test
	void testRemoveThrowsWithoutNext() {
		for (int i = 0; i < 10; ++i) {
			tree.add(i);
			set.add(i);
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		for (int i = 0; i < 4; ++i) {
			assertEquals(iterator1.hasNext(), iterator2.hasNext());
			assertEquals(iterator1.next(), iterator2.next());
		}

		iterator1.remove();
		iterator2.remove();

		assertThrows(IllegalStateException.class, iterator1::remove);
		assertThrows(IllegalStateException.class, iterator2::remove);
	}

	@Test
	void testRetainAll() {
		for (int i = 0; i < 100; ++i) {
			set.add(i);
			tree.add(i);
		}

		Collection<Integer> coll = Arrays.asList(26, 29, 25);

		assertEquals(set.retainAll(coll), tree.retainAll(coll));
		assertEquals(set.size(), tree.size());

		assertTrue(set.containsAll(tree));
		assertTrue(tree.containsAll(set));
	}

	@Test
	void testIteratorRemove() {
		for (int i = 10; i < 16; ++i) {
			assertEquals(set.add(i), tree.add(i));
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		assertEquals(iterator1.hasNext(), iterator2.hasNext());
		assertEquals(iterator1.next(), iterator2.next());

		assertEquals(iterator1.hasNext(), iterator2.hasNext());
		assertEquals(iterator1.next(), iterator2.next());

		iterator1.remove(); // remove 11
		iterator2.remove();

		assertEquals(iterator1.hasNext(), iterator2.hasNext());
		assertEquals(iterator1.next(), iterator2.next());

		assertEquals(iterator1.hasNext(), iterator2.hasNext());
		assertEquals(iterator1.next(), iterator2.next());

		iterator1.remove(); // remove 13
		iterator2.remove();

		assertEquals(set.size(), tree.size());

		for (int i = 10; i < 16; ++i) {
			assertEquals(set.contains(i), tree.contains(i));
		}
	}

	@ParameterizedTest(name = "seed = {0}")
	@MethodSource("seedSource")
	void testIteratorBruteForce(long seed) {
		for (int i = 0; i < 1000; ++i) {
			assertEquals(set.add(i), tree.add(i));
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		Random random = new Random(seed);

		while (true) {
			if (!iterator1.hasNext()) {
				assertFalse(iterator2.hasNext());
				break;
			}

			boolean toRemove = random.nextBoolean();

			if (toRemove) {
				boolean thrown = false;

				try {
					iterator1.remove();
				} catch (IllegalStateException ex) {
					thrown = true;
				}

				if (thrown) {
					assertThrows(IllegalStateException.class, iterator2::remove);
				} else {
					iterator2.remove();
				}
			} else {
				assertEquals(iterator1.hasNext(), iterator2.hasNext());

				if (iterator1.hasNext()) {
					assertEquals(iterator1.next(), iterator2.next());
				} else {
					break;
				}
			}
		}

		assertEquals(set.size(), tree.size());
		assertTrue(tree.isHealthy());
		assertTrue(set.containsAll(tree));
		assertTrue(tree.containsAll(set));
	}

	@Test
	void testIteratorConcurrentModification() {
		for (int i = 0; i < 100; ++i) {
			set.add(i);
			tree.add(i);
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		set.remove(10);
		tree.remove(10);

		assertEquals(iterator1.hasNext(), iterator2.hasNext());

		boolean thrown = false;

		try {
			iterator1.next();
		} catch (ConcurrentModificationException ex) {
			thrown = true;
		}

		if (thrown) {
			assertThrows(ConcurrentModificationException.class, iterator2::next);
		} else {
			iterator2.next();
		}
	}

	@Test
	void testIteratorConcurrentRemove() {
		for (int i = 10; i < 20; ++i) {
			set.add(i);
			tree.add(i);
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		for (int i = 0; i < 4; ++i) {
			iterator1.next();
			iterator2.next();
		}

		// None of them contains 2, should not change the modification count.
		set.remove(2);
		tree.remove(2);

		iterator1.remove();
		iterator2.remove();

		iterator1.next();
		iterator2.next();

		set.remove(12);
		tree.remove(12);

		// Both of them should throw.
		assertThrows(ConcurrentModificationException.class, iterator1::remove);
		assertThrows(ConcurrentModificationException.class, iterator2::remove);
	}

	@Test
	void testConcurrentOrIllegalStateOnRemove() {
		for (int i = 0; i < 10; ++i) {
			set.add(i);
			tree.add(i);
		}

		Iterator<Integer> iterator1 = set.iterator();
		Iterator<Integer> iterator2 = tree.iterator();

		set.add(100);
		tree.add(100);

		assertThrows(IllegalStateException.class, iterator1::remove);
		assertThrows(IllegalStateException.class, iterator2::remove);
	}

	@Test
	void testConcurrentIterators() {
		for (int i = 0; i < 10; ++i) {
			set.add(i);
			tree.add(i);
		}

		Iterator<Integer> iterator1a = set.iterator();
		Iterator<Integer> iterator1b = set.iterator();
		Iterator<Integer> iterator2a = tree.iterator();
		Iterator<Integer> iterator2b = tree.iterator();

		for (int i = 0; i < 3; ++i) {
			iterator1a.next();
			iterator2a.next();
		}

		iterator1a.remove();
		iterator2a.remove();

		assertEquals(iterator1b.hasNext(), iterator2b.hasNext());

		assertThrows(ConcurrentModificationException.class, iterator1b::next);
		assertThrows(ConcurrentModificationException.class, iterator2b::next);
	}

	@ParameterizedTest(name = "seed = {0}")
	@MethodSource("seedSource")
	void testToArray(long seed) {
		Random r = new Random(seed);

		for (int i = 0; i < 50; ++i) {
			int num = r.nextInt();
			set.add(num);
			tree.add(num);
		}

		assertArrayEquals(set.toArray(), tree.toArray());
	}

	@Test
	void testToArrayGeneric() {
		for (int i = 0; i < 100; ++i) {
			set.add(i);
			tree.add(i);
		}

		Integer[] array1before = new Integer[99];
		Integer[] array2before = new Integer[99];

		Integer[] array1after = set.toArray(array1before);
		Integer[] array2after = tree.toArray(array2before);

		assertNotSame(array1before, array1after);
		assertNotSame(array2before, array2after);
		assertArrayEquals(array1after, array2after);

		set.remove(1);
		tree.remove(1);

		array1after = set.toArray(array1before);
		array2after = tree.toArray(array2before);

		assertSame(array1before, array1after);
		assertSame(array2before, array2after);
		assertArrayEquals(array1after, array2after);
	}

	static Stream<Arguments> seedSource() {
		return Stream.of(
				Arguments.of(0L),
				Arguments.of(1L),
				Arguments.of(2L),
				Arguments.of(3L),
				Arguments.of(4L)
		);
	}
}
