/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import org.junit.jupiter.api.Test;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecisionTreeTests {
	@Test
	void initialValueTest() {
		var sut = new DecisionTree(3, TruthValue.UNKNOWN);
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.UNKNOWN));
	}

	@Test
	void mergeValueTest() {
		var sut = new DecisionTree(3, TruthValue.FALSE);
		sut.mergeValue(Tuple.of(3, 4, 5), TruthValue.TRUE);
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.ERROR));
		assertThat(sut.get(Tuple.of(3, 4, 6)), is(TruthValue.FALSE));
	}

	@Test
	void mergeUnknownValueTest() {
		var sut = new DecisionTree(3, TruthValue.FALSE);
		sut.mergeValue(Tuple.of(3, 4, 5), TruthValue.UNKNOWN);
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.FALSE));
	}

	@Test
	void mergeWildcardTest() {
		var sut = new DecisionTree(3, TruthValue.UNKNOWN);
		sut.mergeValue(Tuple.of(3, -1, 5), TruthValue.TRUE);
		sut.mergeValue(Tuple.of(-1, 4, 5), TruthValue.FALSE);
		assertThat(sut.get(Tuple.of(2, 4, 5)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.ERROR));
		assertThat(sut.get(Tuple.of(3, 6, 5)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(3, 4, 6)), is(TruthValue.UNKNOWN));
	}

	@Test
	void mergeWildcardTest2() {
		var sut = new DecisionTree(3, TruthValue.UNKNOWN);
		sut.mergeValue(Tuple.of(-1, 4, -1), TruthValue.FALSE);
		sut.mergeValue(Tuple.of(3, -1, 5), TruthValue.TRUE);
		assertThat(sut.get(Tuple.of(2, 4, 5)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.ERROR));
		assertThat(sut.get(Tuple.of(3, 6, 5)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(3, 4, 6)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 5, 6)), is(TruthValue.UNKNOWN));
	}

	@Test
	void mergeWildcardTest3() {
		var sut = new DecisionTree(3, TruthValue.UNKNOWN);
		sut.mergeValue(Tuple.of(-1, 4, -1), TruthValue.FALSE);
		sut.mergeValue(Tuple.of(3, -1, 5), TruthValue.TRUE);
		sut.mergeValue(Tuple.of(-1, -1, -1), TruthValue.ERROR);
		assertThat(sut.get(Tuple.of(2, 4, 5)), is(TruthValue.ERROR));
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.ERROR));
		assertThat(sut.get(Tuple.of(3, 6, 5)), is(TruthValue.ERROR));
		assertThat(sut.get(Tuple.of(3, 4, 6)), is(TruthValue.ERROR));
		assertThat(sut.get(Tuple.of(3, 5, 6)), is(TruthValue.ERROR));
	}

	@Test
	void mergeOverUnsetTest() {
		var sut = new DecisionTree(3, null);
		sut.mergeValue(Tuple.of(-1, 4, 5), TruthValue.UNKNOWN);
		sut.mergeValue(Tuple.of(3, -1, 5), TruthValue.FALSE);
		assertThat(sut.get(Tuple.of(2, 4, 5)), is(TruthValue.UNKNOWN));
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 6, 5)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 4, 6)), is(nullValue()));
	}

	@Test
	void emptyIterationTest() {
		var sut = new DecisionTree(3, TruthValue.UNKNOWN);
		var map = iterateAll(sut, TruthValue.UNKNOWN, 2);
		assertThat(map.keySet(), hasSize(0));
	}

	@Test
	void completeIterationTest() {
		var sut = new DecisionTree(3, TruthValue.UNKNOWN);
		var map = iterateAll(sut, TruthValue.FALSE, 2);
		assertThat(map.keySet(), hasSize(8));
		assertThat(map, hasEntry(Tuple.of(0, 0, 0), TruthValue.UNKNOWN));
		assertThat(map, hasEntry(Tuple.of(0, 0, 1), TruthValue.UNKNOWN));
		assertThat(map, hasEntry(Tuple.of(0, 1, 0), TruthValue.UNKNOWN));
		assertThat(map, hasEntry(Tuple.of(0, 1, 1), TruthValue.UNKNOWN));
		assertThat(map, hasEntry(Tuple.of(1, 0, 0), TruthValue.UNKNOWN));
		assertThat(map, hasEntry(Tuple.of(1, 0, 1), TruthValue.UNKNOWN));
		assertThat(map, hasEntry(Tuple.of(1, 1, 0), TruthValue.UNKNOWN));
		assertThat(map, hasEntry(Tuple.of(1, 1, 1), TruthValue.UNKNOWN));
	}

	@Test
	void mergedIterationTest() {
		var sut = new DecisionTree(2, TruthValue.UNKNOWN);
		sut.mergeValue(Tuple.of(1, -1), TruthValue.TRUE);
		sut.mergeValue(Tuple.of(-1, 2), TruthValue.FALSE);
		var map = iterateAll(sut, TruthValue.UNKNOWN, 3);
		assertThat(map.keySet(), hasSize(5));
		assertThat(map, hasEntry(Tuple.of(0, 2), TruthValue.FALSE));
		assertThat(map, hasEntry(Tuple.of(1, 0), TruthValue.TRUE));
		assertThat(map, hasEntry(Tuple.of(1, 1), TruthValue.TRUE));
		assertThat(map, hasEntry(Tuple.of(1, 2), TruthValue.ERROR));
		assertThat(map, hasEntry(Tuple.of(2, 2), TruthValue.FALSE));
	}

	@Test
	void sparseIterationTest() {
		var sut = new DecisionTree(2, null);
		sut.mergeValue(Tuple.of(0, 0), TruthValue.TRUE);
		sut.mergeValue(Tuple.of(1, 1), TruthValue.FALSE);
		var map = iterateAll(sut, null, 10);
		assertThat(map.keySet(), hasSize(2));
		assertThat(map, hasEntry(Tuple.of(0, 0), TruthValue.TRUE));
		assertThat(map, hasEntry(Tuple.of(1, 1), TruthValue.FALSE));
	}

	@Test
	void overwriteIterationTest() {
		var sut = new DecisionTree(1, TruthValue.TRUE);
		var overwrite = new DecisionTree(1, null);
		overwrite.mergeValue(Tuple.of(0), TruthValue.UNKNOWN);
		sut.overwriteValues(overwrite);
		var map = iterateAll(sut, TruthValue.UNKNOWN, 2);
		assertThat(map.keySet(), hasSize(1));
		assertThat(map, hasEntry(Tuple.of(1), TruthValue.TRUE));
	}

	@Test
	void overwriteNothingTest() {
		var sut = new DecisionTree(2, TruthValue.UNKNOWN);
		var values = new DecisionTree(2, null);
		sut.overwriteValues(values);
		assertThat(sut.get(Tuple.of(0, 0)), is(TruthValue.UNKNOWN));
	}

	@Test
	void overwriteEverythingTest() {
		var sut = new DecisionTree(2, TruthValue.FALSE);
		sut.mergeValue(Tuple.of(0, 0), TruthValue.ERROR);
		var values = new DecisionTree(2, TruthValue.TRUE);
		sut.overwriteValues(values);
		assertThat(sut.get(Tuple.of(0, 0)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
	}

	@Test
	void overwriteWildcardTest() {
		var sut = new DecisionTree(3, TruthValue.UNKNOWN);
		sut.mergeValue(Tuple.of(1, 1, 1), TruthValue.FALSE);
		sut.mergeValue(Tuple.of(-1, 4, 5), TruthValue.FALSE);
		sut.mergeValue(Tuple.of(3, -1, 5), TruthValue.TRUE);
		var values = new DecisionTree(3, null);
		values.mergeValue(Tuple.of(2, 2, 2), TruthValue.TRUE);
		values.mergeValue(Tuple.of(-1, 4, 5), TruthValue.UNKNOWN);
		values.mergeValue(Tuple.of(3, -1, 5), TruthValue.FALSE);
		sut.overwriteValues(values);
		assertThat(sut.get(Tuple.of(1, 1, 1)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(2, 2, 2)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(2, 4, 5)), is(TruthValue.UNKNOWN));
		assertThat(sut.get(Tuple.of(3, 4, 5)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 6, 5)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 4, 6)), is(TruthValue.UNKNOWN));
	}

	@Test
	void reducedValueEmptyTest() {
		var sut = new DecisionTree(2, TruthValue.TRUE);
		assertThat(sut.getReducedValue(), is(TruthValue.TRUE));
	}

	@Test
	void reducedValueUnsetTest() {
		var sut = new DecisionTree(2);
		assertThat(sut.getReducedValue(), is(nullValue()));
	}

	@Test
	void reducedValueNonEmptyTest() {
		var sut = new DecisionTree(2, TruthValue.UNKNOWN);
		sut.mergeValue(Tuple.of(1, 2), TruthValue.TRUE);
		assertThat(sut.getReducedValue(), is(nullValue()));
	}

	@Test
	void removeIntermediateChildTest() {
		var sut = new DecisionTree(3, TruthValue.TRUE);
		var values = new DecisionTree(3, null);
		values.mergeValue(Tuple.of(1, 1, 1), TruthValue.UNKNOWN);
		sut.overwriteValues(values);
		sut.mergeValue(Tuple.of(1, 1, 1), TruthValue.TRUE);
		assertThat(sut.get(Tuple.of(1, 1, 1)), is(TruthValue.TRUE));
		assertThat(sut.getReducedValue(), is(TruthValue.TRUE));
	}

	@Test
	void setMissingValueTest() {
		var sut = new DecisionTree(2);
		sut.setIfMissing(Tuple.of(0, 0), TruthValue.FALSE);
		assertThat(sut.get(Tuple.of(0, 0)), is(TruthValue.FALSE));
	}

	@Test
	void setNotMissingValueTest() {
		var sut = new DecisionTree(2);
		sut.mergeValue(Tuple.of(0, 0), TruthValue.TRUE);
		sut.setIfMissing(Tuple.of(0, 0), TruthValue.FALSE);
		assertThat(sut.get(Tuple.of(0, 0)), is(TruthValue.TRUE));
	}

	@Test
	void setNotMissingDefaultValueTest() {
		var sut = new DecisionTree(2, TruthValue.TRUE);
		sut.setIfMissing(Tuple.of(0, 0), TruthValue.FALSE);
		assertThat(sut.get(Tuple.of(0, 0)), is(TruthValue.TRUE));
	}

	@Test
	void setMissingValueWildcardTest() {
		var sut = new DecisionTree(2);
		sut.mergeValue(Tuple.of(-1, 0), TruthValue.TRUE);
		sut.mergeValue(Tuple.of(1, -1), TruthValue.TRUE);
		sut.setIfMissing(Tuple.of(0, 0), TruthValue.FALSE);
		sut.setIfMissing(Tuple.of(1, 1), TruthValue.FALSE);
		sut.setIfMissing(Tuple.of(2, 2), TruthValue.FALSE);
		assertThat(sut.get(Tuple.of(0, 0)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(1, 1)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(2, 2)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(2, 3)), is(nullValue()));
	}

	@Test
	void setMissingValueInvalidTupleTest() {
		var sut = new DecisionTree(2);
		var tuple = Tuple.of(-1, -1);
		assertThrows(IllegalArgumentException.class, () -> sut.setIfMissing(tuple, TruthValue.TRUE));
	}

	@Test
	void setAllMissingTest() {
		var sut = new DecisionTree(2);
		sut.mergeValue(Tuple.of(-1, 0), TruthValue.TRUE);
		sut.mergeValue(Tuple.of(1, -1), TruthValue.TRUE);
		sut.mergeValue(Tuple.of(2, 2), TruthValue.TRUE);
		sut.setAllMissing(TruthValue.FALSE);
		assertThat(sut.get(Tuple.of(0, 0)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(2, 0)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(1, 1)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(1, 2)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(2, 2)), is(TruthValue.TRUE));
		assertThat(sut.get(Tuple.of(2, 3)), is(TruthValue.FALSE));
		assertThat(sut.get(Tuple.of(3, 2)), is(TruthValue.FALSE));
	}

	@Test
	void setAllMissingEmptyTest() {
		var sut = new DecisionTree(2);
		sut.setAllMissing(TruthValue.TRUE);
		assertThat(sut.getReducedValue(), is(TruthValue.TRUE));
	}

	@Test
	void overwriteWildcardAllTest() {
		var first = new DecisionTree(2, TruthValue.UNKNOWN);
		first.mergeValue(Tuple.of(-1, -1), TruthValue.FALSE);
		var second = new DecisionTree(2, null);
		second.mergeValue(Tuple.of(1, -1), TruthValue.TRUE);
		first.overwriteValues(second);
		assertThat(first.majorityValue(), is(TruthValue.FALSE));
	}

	private Map<Tuple, TruthValue> iterateAll(DecisionTree sut, TruthValue defaultValue, int nodeCount) {
		var cursor = sut.getCursor(defaultValue, nodeCount);
		var map = new LinkedHashMap<Tuple, TruthValue>();
		while (cursor.move()) {
			map.put(cursor.getKey(), cursor.getValue());
		}
		assertThat(cursor.isDirty(), is(false));
		assertThat(cursor.isTerminated(), is(true));
		return map;
	}
}
