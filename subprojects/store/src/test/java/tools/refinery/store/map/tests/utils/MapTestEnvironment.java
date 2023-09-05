/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.tests.utils;

import tools.refinery.store.map.*;
import tools.refinery.store.map.internal.state.VersionedMapStateImpl;

import java.util.*;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.*;

public class MapTestEnvironment<K, V> {
	public static String[] prepareValues(int maxValue, boolean nullDefault) {
		String[] values = new String[maxValue];
		if (nullDefault) {
			values[0] = null;
		} else {
			values[0] = "DEFAULT";
		}

		for (int i = 1; i < values.length; i++) {
			values[i] = "VAL" + i;
		}
		return values;
	}

	public static ContinuousHashProvider<Integer> prepareHashProvider(final boolean evil) {
		// Use maxPrime = 2147483629

		return (key, index) -> {
			if (evil && index < 15 && index < key / 3) {
				return 7;
			}
			int result = 1;
			final int prime = 31;

			result = prime * result + key;
			result = prime * result + index;

			return result;
		};
	}

	public static void printStatus(String scenario, int actual, int max, String stepName) {
		if (actual % 10000 == 0) {
			String printStepName = stepName == null ? "" : stepName;
			System.out.format(scenario + ":%d/%d (%d%%) " + printStepName + "%n", actual, max, actual * 100 / max);
		}

	}

	public static <K, V> void compareTwoMaps(String title, VersionedMap<K, V> map1,
											 VersionedMap<K, V> map2) {
		compareTwoMaps(title, map1, map2, null);
	}

	public static <K, V> void compareTwoMaps(String title, VersionedMap<K, V> map1,
											 VersionedMap<K, V> map2, List<Throwable> errors) {
		map1.checkIntegrity();
		map2.checkIntegrity();

		assertContentEqualsList(map1, map2, title + ": map1.contentEquals(map2)", errors);
		assertContentEqualsList(map2, map1, title + ": map2.contentEquals(map1)", errors);
		assertEqualsList(map1.getSize(), map2.getSize(), title + ": Sizes not equal", errors);

		for (var mode : ContentHashCode.values()) {
			assertEqualsList(map1.contentHashCode(mode), map2.contentHashCode(mode),
					title + ": " + mode + " hashCode check", errors);
		}
	}

	private static void assertEqualsList(Object o1, Object o2, String message, List<Throwable> errors) {
		if (errors == null) {
			assertEquals(o1, o2, message);
		} else {
			if (o1 != null) {
				if (!(o1.equals(o2))) {
					AssertionError error =
							new AssertionError((message != null ? message + " " : "") + "expected: " + o1 + " but was " +
									": " + o2);
					errors.add(error);
				}
			}
		}
	}

	private static void assertContentEqualsList(AnyVersionedMap o1, AnyVersionedMap o2, String message,
												List<Throwable> errors) {
		boolean result = o1.contentEquals(o2);
		if (errors == null) {
			assertTrue(result, message);
		} else if (!result) {
			AssertionError error =
					new AssertionError((message != null ? message + " " : "") + "expected: true but was: false");
			errors.add(error);
		}
	}

	final private VersionedMap<K, V> sut;
	final private V defaultValue;
	Map<K, V> oracle = new HashMap<>();

	public MapTestEnvironment(VersionedMap<K, V> sut) {
		this.sut = sut;
		this.defaultValue = sut.getDefaultValue();
	}

	public void put(K key, V value) {
		V oldSutValue = sut.put(key, value);
		V oldOracleValue;
		if (value != defaultValue) {
			oldOracleValue = oracle.put(key, value);
		} else {
			oldOracleValue = oracle.remove(key);
		}
		if (oldSutValue == defaultValue && oldOracleValue != null) {
			fail("After put, SUT old nodeId was default, but oracle old value was " + oldOracleValue);
		}
		if (oldSutValue != defaultValue) {
			assertEquals(oldOracleValue, oldSutValue);
		}
	}

	public Version commit(){
		return sut.commit();
	}

	public void checkEquivalence(String title) {
		// 0. Checking integrity
		try {
			sut.checkIntegrity();
		} catch (IllegalStateException e) {
			fail(title + ":  " + e.getMessage());
		}

		// 1. Checking: if Reference contains <key,nodeId> pair, then SUT contains
		// <key,nodeId> pair.
		// Tests get functions
		for (Entry<K, V> entry : oracle.entrySet()) {
			V sutValue = sut.get(entry.getKey());
			V oracleValue = entry.getValue();
			if (sutValue != oracleValue) {
				printComparison();
				fail(title + ": Non-equivalent get(" + entry.getKey() + ") results: SUT=" + sutValue + ", Oracle="
						+ oracleValue + "!");
			}
		}

		// 2. Checking: if SUT contains <key,nodeId> pair, then Reference contains
		// <key,nodeId> pair.
		// Tests iterators
		int elementsInSutEntrySet = 0;
		Cursor<K, V> cursor = sut.getAll();
		while (cursor.move()) {
			elementsInSutEntrySet++;
			K key = cursor.getKey();
			V sutValue = cursor.getValue();
			// System.out.println(key + " -> " + sutValue);
			V oracleValue = oracle.get(key);
			if (sutValue != oracleValue) {
				printComparison();
				fail(title + ": Non-equivalent entry in iterator: SUT=<" + key + "," + sutValue + ">, Oracle=<" + key
						+ "," + oracleValue + ">!");
			}

		}

		// 3. Checking sizes
		// Counting of non-default nodeId pairs.
		int oracleSize = oracle.entrySet().size();
		long sutSize = sut.getSize();
		if (oracleSize != sutSize || oracleSize != elementsInSutEntrySet) {
			printComparison();
			fail(title + ": Non-equivalent size() result: SUT.getSize()=" + sutSize + ", SUT.entrySet.size="
					+ elementsInSutEntrySet + ", Oracle=" + oracleSize + "!");
		}
	}

	public static <K, V> void checkOrder(String scenario, VersionedMap<K, V> versionedMap) {
		K previous = null;
		Cursor<K, V> cursor = versionedMap.getAll();
		while (cursor.move()) {
			//System.out.println(cursor.getKey() + " " + ((VersionedMapImpl<K, V>) versionedMap).getHashProvider()
			// .getHash(cursor.getKey(), 0));
			if (previous != null) {
				int comparisonResult = ((VersionedMapStateImpl<K, V>) versionedMap).getHashProvider().compare(previous,
						cursor.getKey());
				assertTrue(comparisonResult < 0, scenario + " Cursor order is not incremental!");
			}
			previous = cursor.getKey();
		}
	}

	public void printComparison() {
		System.out.println("SUT:");
		printEntrySet(sut.getAll());
		System.out.println("Oracle:");
		printEntrySet(oracle.entrySet().iterator());
	}

	private void printEntrySet(Iterator<Entry<K, V>> iterator) {
		Map<K, V> map = new LinkedHashMap<>();
		while (iterator.hasNext()) {
			Entry<K, V> entry = iterator.next();
			map.put(entry.getKey(), entry.getValue());
		}
		for (Entry<K, V> e : map.entrySet()) {
			System.out.println("\t" + e.getKey() + " -> " + e.getValue());
		}
	}

	private void printEntrySet(Cursor<K, V> cursor) {
		Map<K, V> map = new LinkedHashMap<>();
		while (cursor.move()) {
			map.put(cursor.getKey(), cursor.getValue());
		}
		for (Entry<K, V> e : map.entrySet()) {
			System.out.println("\t" + e.getKey() + " -> " + e.getValue());
		}
	}
}
