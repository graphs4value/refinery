/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class ActivationUnitTest {
	private final static int SMALL_SIZE = 5;

	private static Stream<ActivationStoreEntry> entries() {
		return Stream.of(
				new ActivationStoreBitVectorEntry(SMALL_SIZE),
				new ActivationStoreListEntry(SMALL_SIZE));
	}

	void addTest(ActivationStoreEntry entry, int elementsAdded) {
		Assertions.assertEquals(elementsAdded,entry.getNumberOfVisitedActivations());
		Assertions.assertEquals(SMALL_SIZE-elementsAdded,entry.getNumberOfUnvisitedActivations());
	}

	@ParameterizedTest
	@MethodSource("entries")
	void testDifferent(ActivationStoreEntry entry) {
		int elementsAdded = 0;
		addTest(entry,elementsAdded);
		Assertions.assertEquals(2, entry.getAndAddActivationAfter(2));
		addTest(entry,++elementsAdded);
		Assertions.assertEquals(3,entry.getAndAddActivationAfter(3));
		addTest(entry,++elementsAdded);
		Assertions.assertEquals(1,entry.getAndAddActivationAfter(1));
		addTest(entry,++elementsAdded);
		Assertions.assertEquals(4,entry.getAndAddActivationAfter(4));
		addTest(entry,++elementsAdded);
		Assertions.assertEquals(0,entry.getAndAddActivationAfter(0));
		addTest(entry,++elementsAdded);
	}

	@ParameterizedTest
	@MethodSource("entries")
	void testSame(ActivationStoreEntry entry) {
		int elementsAdded = 0;
		addTest(entry,elementsAdded);
		entry.getAndAddActivationAfter(2);
		addTest(entry,++elementsAdded);
		entry.getAndAddActivationAfter(2);
		addTest(entry,++elementsAdded);
		entry.getAndAddActivationAfter(2);
		addTest(entry,++elementsAdded);
		entry.getAndAddActivationAfter(2);
		addTest(entry,++elementsAdded);
		entry.getAndAddActivationAfter(2);
		addTest(entry,++elementsAdded);
	}

	@ParameterizedTest
	@MethodSource("entries")
	void testFilling(ActivationStoreEntry entry) {
		int elementsAdded = 0;
		while(elementsAdded < SMALL_SIZE) {
			entry.getAndAddActivationAfter(2);
			elementsAdded++;
		}
		Assertions.assertThrows(IllegalArgumentException.class,()-> entry.getAndAddActivationAfter(2));
	}
}
