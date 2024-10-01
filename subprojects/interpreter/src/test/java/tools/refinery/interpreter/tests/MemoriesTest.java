/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.interpreter.matchers.util.IMemory;
import tools.refinery.interpreter.matchers.util.IMemoryView;
import tools.refinery.interpreter.matchers.util.ISetMemory;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

/**
 * Tests the behaviour of various {@link IMemory} implementations.
 * @author Gabor Bergmann
 *
 * TODO untested: clearAllOf, addSigned, specialized memory views.
 */
@RunWith(Parameterized.class)
public class MemoriesTest<V> {

	private static final String NEVER_USED_STRING = "NEVER_USED_STRING";
	private static final Long NEVER_USED_LONG = -42L;

	private static final int POOL_LIMIT = 7;
	// indices (1 through 7 allowed)        0     1      2      3      4         5         6          7
	private final static String[] STRING_POOL = {  null, "foo", "bar", "baz", "tinker", "tailor", "soldier", "sailor"};
	private final static Long[]   LONG_POOL   = {  null, 100L,  200L,  300L,  400L,     500L,     600L,      700L};


	@Parameters(name= "{index}: {0} storage")
	public static Collection<Object[]> data() {
		return Arrays.asList(
				new Object[] {Object.class, STRING_POOL, NEVER_USED_STRING},
				new Object[] {Long.class,   LONG_POOL,   NEVER_USED_LONG}
		);
	}

	@Parameter(0)
	public Class<? super V> storageClass;
	@Parameter(1)
	public V[] objectPool;
	@Parameter(2)
	public V neverUsed;

	private String stepPrefix;

	private interface Step<V> extends BiConsumer<IMemory<V>, Map<V, Integer>> {

	}


	/*
	 *  operations:
	 *    - positive integers between 1 and POOL_LIMIT denote insertion of the selected index from the object pool,
	 *    - negative integers denote removal of their inverse,
	 *    - zero is disallowed
	 */
	private final Step<?>[] OPERATION_SEQ_1 = { // works for all
			_addOne(1), _addOne(2), _addOne(3), _addOne(4), _addOne(5), _addOne(6), _addOne(7), _removeOne(1), _clearAllOf(2), _clear()};
	private final Step<?>[] OPERATION_SEQ_2 = { // works for all
			_addOne(1), _addOne(2), _addOne(3), _removeOne(2), _addOne(2), _removeOne(2), _removeOne(3), _removeOne(1), _clear(),
			_addOne(1), _addOne(2), _addOne(3), _addOne(2), _addSigned(3,1), _clearAllOf(2), _clearAllOf(3), _removeOne(1), _clear(),
			_addOne(1), _addSigned(2,1), _addOne(4), _addOne(5), _addSigned(6,+1), _addOne(5), _removeOne(6), _addSigned(5,-1),
			_clearAllOf(3), _addOne(3), _clearAllOf(2), _addOne(2), _clearAllOf(6), _addOne(6), _clear()};
	private final Step<?>[] OPERATION_SEQ_3 = { // overflows set memory
			_addOne(1), _addOne(7), _addOne(7), _removeOne(7), _removeOne(7),
			_addSigned(2,9), _addOne(4), _addOne(5), _addSigned(6,2), _addOne(5), _removeOne(6), _removeOne(5), _removeOne(6),
			_clearAllOf(3), _addOne(3), _clearAllOf(2), _addOne(2), _clearAllOf(6), _addOne(6), _clear()};
	private final Step<?>[] OPERATION_SEQ_4 = { // overflows set&multiset memory
			_addOne(1), _removeOne(4), _addSigned(4, -2), _addSigned(2, -9), _removeOne(7), _removeOne(7), _addOne(7), _addOne(7),
			_addOne(3), _addSigned(3, -9), _addSigned(3, +8), _clearAllOf(4), _addOne(4), _clearAllOf(4), _removeOne(4), _clear()};



	@Test
	public void setMemorySeq1() {
		IMemory<V> memory = CollectionsFactory.createMemory(storageClass, MemoryType.SETS);
		performSequence("setMemorySeq1",
				memory, OPERATION_SEQ_1);
	}
	@Test
	public void setMemorySeq2() {
		IMemory<V> memory = CollectionsFactory.createMemory(storageClass, MemoryType.SETS);
		performSequence("setMemorySeq2",
				memory, OPERATION_SEQ_2);
	}
	@Test(expected = IllegalStateException.class)
	public void setMemorySeq3() {
		IMemory<V> memory = CollectionsFactory.createMemory(storageClass, MemoryType.SETS);
		performSequence("setMemorySeq3",
				memory, OPERATION_SEQ_3);
	}
	@Test(expected = IllegalStateException.class)
	public void setMemorySeq4() {
		IMemory<V> memory = CollectionsFactory.createMemory(storageClass, MemoryType.SETS);
		performSequence("multisetSeq4",
				memory, OPERATION_SEQ_4);
	}
	@Test
	public void multisetSeq1() {
		IMemory<V> memory = CollectionsFactory.createMultiset();
		performSequence("multisetSeq1",
				memory, OPERATION_SEQ_1);
	}
	@Test
	public void multisetSeq2() {
		IMemory<V> memory = CollectionsFactory.createMultiset();
		performSequence("multisetSeq2",
				memory, OPERATION_SEQ_2);
	}
	@Test
	public void multisetSeq3() {
		IMemory<V> memory = CollectionsFactory.createMultiset();
		performSequence("multisetSeq3",
				memory, OPERATION_SEQ_3);
	}
	@Test(expected = IllegalStateException.class)
	public void multisetSeq4() {
		IMemory<V> memory = CollectionsFactory.createMultiset();
		performSequence("multisetSeq4",
				memory, OPERATION_SEQ_4);
	}
	@Test
	public void deltaMemorySeq1() {
		if (storageClass != Object.class) return; // not implemented, ignore
		IMemory<V> memory = CollectionsFactory.createDeltaBag();
		performSequence("deltaMemorySeq1",
				memory, OPERATION_SEQ_1);
	}
	@Test
	public void deltaMemorySeq2() {
		if (storageClass != Object.class) return; // not implemented, ignore
		IMemory<V> memory = CollectionsFactory.createDeltaBag();
		performSequence("deltaMemorySeq2",
				memory, OPERATION_SEQ_2);
	}
	@Test
	public void deltaMemorySeq3() {
		if (storageClass != Object.class) return; // not implemented, ignore
		IMemory<V> memory = CollectionsFactory.createDeltaBag();
		performSequence("deltaMemorySeq3",
				memory, OPERATION_SEQ_3);
	}
	@Test
	public void deltaMemorySeq4() {
		if (storageClass != Object.class) return; // not implemented, ignore
		IMemory<V> memory = CollectionsFactory.createDeltaBag();
		performSequence("deltaMemorySeq4",
				memory, OPERATION_SEQ_4);
	}

	public void performSequence(String casePrefix, IMemory<V> memory, Step<?>[] operations) {
		assertEquals(POOL_LIMIT + 1, objectPool.length);
		String seqPrefix = storageClass.getSimpleName()+'.'+casePrefix;

		Map<V, Integer> expected = new HashMap<>();

		for (int opIndex = 0; opIndex < operations.length; ++opIndex) {
			@SuppressWarnings("unchecked")
			Step<V> op = (Step<V>)operations[opIndex];

			stepPrefix = String.format("%s[%d]", seqPrefix, opIndex);

			op.accept(memory, expected);
		}
	}

	public Step<V> _addOne(int selectorIndex) {
		assertTrue(selectorIndex > 0);
		assertFalse(selectorIndex > POOL_LIMIT);
		return (memory, expected) -> {
			V selectedValue = objectPool[selectorIndex];

			// REPLICATE MEMORY BEHAVIOUR (non-exception case)
			Integer previous = expected.putIfAbsent(selectedValue, 0);
			boolean wasAbsent = (previous == null);
			int newCount = expected.get(selectedValue) + 1;
			int newCountNormalized = (memory instanceof ISetMemory<?>) ? Math.min(1, newCount) : newCount;
			boolean nowAbsent = (newCount == 0);
			if (nowAbsent) expected.remove(selectedValue); else expected.put(selectedValue, newCountNormalized);

			// PERFORM ACTUAL OPERATION
			boolean returned = memory.addOne(selectedValue);

			// CHECK ASSERTIONS
			String messagePrefix = String.format("%s-addOne(%d=%s)", stepPrefix, selectorIndex, selectedValue);
			assertEquals(messagePrefix+":return", nowAbsent || wasAbsent, returned);
			makeStateAssertions(messagePrefix, memory, expected);
		};
	}

	public Step<V> _addSigned(int selectorIndex, int count) {
		assertTrue(selectorIndex > 0);
		assertFalse(selectorIndex > POOL_LIMIT);
		return (memory, expected) -> {
			V selectedValue = objectPool[selectorIndex];

			// REPLICATE MEMORY BEHAVIOUR (non-exception case)
			Integer previous = expected.putIfAbsent(selectedValue, 0);
			boolean wasAbsent = (previous == null);
			int newCount = expected.get(selectedValue) + count;
			int newCountNormalized = (memory instanceof ISetMemory<?>) ? Math.min(1, newCount) : newCount;
			boolean nowAbsent = (newCount == 0);
			if (nowAbsent) expected.remove(selectedValue); else expected.put(selectedValue, newCountNormalized);

			// PERFORM ACTUAL OPERATION
			boolean returned = memory.addSigned(selectedValue, count);

			// CHECK ASSERTIONS
			String messagePrefix = String.format("%s-addSigned(%d=%s)", stepPrefix, selectorIndex, selectedValue);
			assertEquals(messagePrefix+":return", nowAbsent || wasAbsent, returned);
			makeStateAssertions(messagePrefix, memory, expected);
		};
	}

	public Step<V> _removeOne(int selectorIndex) {
		assertTrue(selectorIndex > 0);
		assertFalse(selectorIndex > POOL_LIMIT);
		return (memory, expected) -> {
			V selectedValue = objectPool[selectorIndex];

			// REPLICATE MEMORY BEHAVIOUR (non-exception case)
			Integer previous = expected.putIfAbsent(selectedValue, 0);
			boolean wasAbsent = (previous == null);
			int newCount = expected.get(selectedValue) - 1;
			int newCountNormalized = (memory instanceof ISetMemory<?>) ? Math.min(1, newCount) : newCount;
			boolean nowAbsent = (newCount == 0);
			if (nowAbsent) expected.remove(selectedValue); else expected.put(selectedValue, newCountNormalized);

			// PERFORM ACTUAL OPERATION
			boolean returned = memory.removeOne(selectedValue);

			// CHECK ASSERTIONS
			String messagePrefix = String.format("%s-removeOne(%d=%s)", stepPrefix, selectorIndex, selectedValue);
			assertEquals(messagePrefix+":return", nowAbsent || wasAbsent, returned);
			makeStateAssertions(messagePrefix, memory, expected);
		};
	}

	public Step<V> _clear() {
		return (memory, expected) -> {
			// REPLICATE MEMORY BEHAVIOUR (non-exception case)
			expected.clear();

			// PERFORM ACTUAL OPERATION
			memory.clear();

			// CHECK ASSERTIONS
			String messagePrefix = String.format("%s-clear()", stepPrefix);
			makeStateAssertions(messagePrefix, memory, expected);
		};
	}

	public Step<V> _clearAllOf(int selectorIndex) {
		assertTrue(selectorIndex > 0);
		assertFalse(selectorIndex > POOL_LIMIT);
		return (memory, expected) -> {
			V selectedValue = objectPool[selectorIndex];

			// REPLICATE MEMORY BEHAVIOUR (non-exception case)
			expected.remove(selectedValue);

			// PERFORM ACTUAL OPERATION
			memory.clearAllOf(selectedValue);

			// CHECK ASSERTIONS
			String messagePrefix = String.format("%s_clearAllOf(%d=%s)", stepPrefix, selectorIndex, selectedValue);
			makeStateAssertions(messagePrefix, memory, expected);
		};
	}

	/**
	 * PRE: expected associates non-zero keys only
	 * PRE: neverUsed never occurs in expected
	 */
	public void makeStateAssertions(String messagePrefix, IMemoryView<V> actual, Map<V, Integer> expected) {

		// GLOBAL API METHODS

		assertEquals(messagePrefix+":asMap",
				expected, actual.asMap());
		assertEquals(messagePrefix+":fromMap",
				IMemoryView.fromMap(expected), actual);

		assertEquals(messagePrefix+":distinctValues",
				expected.keySet(), actual.distinctValues());

		assertEquals(messagePrefix+":isEmpty",
				expected.isEmpty(), actual.isEmpty());
		assertEquals(messagePrefix+":size",
				expected.size(), actual.size());

		// ITERATING METHODS

		Set<V> valuesCopy = new HashSet<>();
		Iterator<V> actualIterator = actual.iterator();
		while(actualIterator.hasNext()) {
			V nextValue = actualIterator.next();
			boolean iteratedPreviously = !valuesCopy.add(nextValue);
			assertFalse(messagePrefix+":iterator/"+ nextValue, iteratedPreviously);
		}
		assertEquals(messagePrefix+":iterator",
				expected.keySet(), valuesCopy);

		Map<V, Integer> entriesCopy = new HashMap<>();
		for (Entry<V, Integer> entry : actual.entriesWithMultiplicities()) {
			Integer previous = entriesCopy.put(entry.getKey(), entry.getValue());
			assertNull(messagePrefix+":entry/" + entry, previous);
		}
		assertEquals(messagePrefix+":entriesWithMultiplicities",
				expected, entriesCopy);

		final Map<V, Integer> entriesCopy2 = new HashMap<>();
		actual.forEachEntryWithMultiplicities((value, count) -> {
			Integer previous = entriesCopy2.put(value, count);
			assertNull(messagePrefix+":entry-consumer/"+ value+'*'+count, previous);
		});
		assertEquals(messagePrefix+":forEachEntryWithMultiplicities",
				expected, entriesCopy2);

		// ELEMENTWISE LOOKUP METHODS, EXPECTED ELEMENTS

		for (Entry<V, Integer> expectedEntry : expected.entrySet()) {
			V expectedValue = expectedEntry.getKey();
			int expectedCount = expectedEntry.getValue();

			assertTrue(messagePrefix+":containsNonZero/"+expectedValue,
					actual.containsNonZero(expectedValue));
			assertTrue(messagePrefix+":containsNonZeroUnsafe/"+expectedValue,
					actual.containsNonZeroUnsafe(expectedValue));
			assertEquals(messagePrefix+":getCount/"+expectedValue,
					expectedCount, actual.getCount(expectedValue));
			assertEquals(messagePrefix+":getCountUnsafe/"+expectedValue,
					expectedCount, actual.getCountUnsafe(expectedValue));
			assertEquals(messagePrefix+":theContainedVersionOf/"+expectedValue,
					expectedValue, actual.theContainedVersionOf(expectedValue));
			assertEquals(messagePrefix+":theContainedVersionOfUnsafe/"+expectedValue,
					expectedValue, actual.theContainedVersionOfUnsafe(expectedValue));

		}

		// ELEMENTWISE LOOKUP METHODS, UNEXPECTED ELEMENTS

		assertFalse(messagePrefix+":containsNonZero/never="+neverUsed,
				actual.containsNonZero(neverUsed));
		assertFalse(messagePrefix+":containsNonZeroUnsafe/never="+neverUsed,
				actual.containsNonZeroUnsafe(neverUsed));
		assertEquals(messagePrefix+":getCount/never="+neverUsed,
				0, actual.getCount(neverUsed));
		assertEquals(messagePrefix+":getCountUnsafe/never="+neverUsed,
				0, actual.getCountUnsafe(neverUsed));
		assertNull(messagePrefix+":theContainedVersionOf/never="+neverUsed,
				actual.theContainedVersionOf(neverUsed));
		assertNull(messagePrefix+":theContainedVersionOfUnsafe/never="+neverUsed,
				actual.theContainedVersionOfUnsafe(neverUsed));
	}


}
