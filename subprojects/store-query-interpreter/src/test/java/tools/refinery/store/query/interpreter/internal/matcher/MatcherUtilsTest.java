/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.matcher;

import tools.refinery.interpreter.matchers.tuple.*;
import org.junit.jupiter.api.Test;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatcherUtilsTest {
	@Test
	void toViatra0Test() {
		var viatraTuple = MatcherUtils.toViatraTuple(Tuple.of());
		assertThat(viatraTuple.getSize(), is(0));
		assertThat(viatraTuple, instanceOf(FlatTuple0.class));
	}

	@Test
	void toViatra1Test() {
		var viatraTuple = MatcherUtils.toViatraTuple(Tuple.of(2));
		assertThat(viatraTuple.getSize(), is(1));
		assertThat(viatraTuple.get(0), is(Tuple.of(2)));
		assertThat(viatraTuple, instanceOf(FlatTuple1.class));
	}

	@Test
	void toViatra2Test() {
		var viatraTuple = MatcherUtils.toViatraTuple(Tuple.of(2, 3));
		assertThat(viatraTuple.getSize(), is(2));
		assertThat(viatraTuple.get(0), is(Tuple.of(2)));
		assertThat(viatraTuple.get(1), is(Tuple.of(3)));
		assertThat(viatraTuple, instanceOf(FlatTuple2.class));
	}

	@Test
	void toViatra3Test() {
		var viatraTuple = MatcherUtils.toViatraTuple(Tuple.of(2, 3, 5));
		assertThat(viatraTuple.getSize(), is(3));
		assertThat(viatraTuple.get(0), is(Tuple.of(2)));
		assertThat(viatraTuple.get(1), is(Tuple.of(3)));
		assertThat(viatraTuple.get(2), is(Tuple.of(5)));
		assertThat(viatraTuple, instanceOf(FlatTuple3.class));
	}

	@Test
	void toViatra4Test() {
		var viatraTuple = MatcherUtils.toViatraTuple(Tuple.of(2, 3, 5, 8));
		assertThat(viatraTuple.getSize(), is(4));
		assertThat(viatraTuple.get(0), is(Tuple.of(2)));
		assertThat(viatraTuple.get(1), is(Tuple.of(3)));
		assertThat(viatraTuple.get(2), is(Tuple.of(5)));
		assertThat(viatraTuple.get(3), is(Tuple.of(8)));
		assertThat(viatraTuple, instanceOf(FlatTuple4.class));
	}

	@Test
	void toViatra5Test() {
		var viatraTuple = MatcherUtils.toViatraTuple(Tuple.of(2, 3, 5, 8, 13));
		assertThat(viatraTuple.getSize(), is(5));
		assertThat(viatraTuple.get(0), is(Tuple.of(2)));
		assertThat(viatraTuple.get(1), is(Tuple.of(3)));
		assertThat(viatraTuple.get(2), is(Tuple.of(5)));
		assertThat(viatraTuple.get(3), is(Tuple.of(8)));
		assertThat(viatraTuple.get(4), is(Tuple.of(13)));
		assertThat(viatraTuple, instanceOf(FlatTuple.class));
	}

	@Test
	void toRefinery0Test() {
		var refineryTuple = MatcherUtils.toRefineryTuple(Tuples.flatTupleOf());
		assertThat(refineryTuple.getSize(), is(0));
		assertThat(refineryTuple, instanceOf(Tuple0.class));
	}

	@Test
	void toRefinery1Test() {
		var refineryTuple = MatcherUtils.toRefineryTuple(Tuples.flatTupleOf(Tuple.of(2)));
		assertThat(refineryTuple.getSize(), is(1));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple, instanceOf(Tuple1.class));
	}

	@Test
	void toRefinery2Test() {
		var refineryTuple = MatcherUtils.toRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3)));
		assertThat(refineryTuple.getSize(), is(2));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple, instanceOf(Tuple2.class));
	}

	@Test
	void toRefinery3Test() {
		var refineryTuple = MatcherUtils.toRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3), Tuple.of(5)));
		assertThat(refineryTuple.getSize(), is(3));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple.get(2), is(5));
		assertThat(refineryTuple, instanceOf(Tuple3.class));
	}

	@Test
	void toRefinery4Test() {
		var refineryTuple = MatcherUtils.toRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3), Tuple.of(5),
				Tuple.of(8)));
		assertThat(refineryTuple.getSize(), is(4));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple.get(2), is(5));
		assertThat(refineryTuple.get(3), is(8));
		assertThat(refineryTuple, instanceOf(Tuple4.class));
	}

	@Test
	void toRefinery5Test() {
		var refineryTuple = MatcherUtils.toRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3), Tuple.of(5),
				Tuple.of(8), Tuple.of(13)));
		assertThat(refineryTuple.getSize(), is(5));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple.get(2), is(5));
		assertThat(refineryTuple.get(3), is(8));
		assertThat(refineryTuple.get(4), is(13));
		assertThat(refineryTuple, instanceOf(TupleN.class));
	}

	@Test
	void toRefineryInvalidValueTest() {
		var viatraTuple = Tuples.flatTupleOf(Tuple.of(2), -98);
		assertThrows(IllegalArgumentException.class, () -> MatcherUtils.toRefineryTuple(viatraTuple));
	}

	@Test
	void keyToRefinery0Test() {
		var refineryTuple = MatcherUtils.keyToRefineryTuple(Tuples.flatTupleOf(-99));
		assertThat(refineryTuple.getSize(), is(0));
		assertThat(refineryTuple, instanceOf(Tuple0.class));
	}

	@Test
	void keyToRefinery1Test() {
		var refineryTuple = MatcherUtils.keyToRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), -99));
		assertThat(refineryTuple.getSize(), is(1));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple, instanceOf(Tuple1.class));
	}

	@Test
	void keyToRefinery2Test() {
		var refineryTuple = MatcherUtils.keyToRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3), -99));
		assertThat(refineryTuple.getSize(), is(2));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple, instanceOf(Tuple2.class));
	}

	@Test
	void keyToRefinery3Test() {
		var refineryTuple = MatcherUtils.keyToRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3), Tuple.of(5),
				-99));
		assertThat(refineryTuple.getSize(), is(3));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple.get(2), is(5));
		assertThat(refineryTuple, instanceOf(Tuple3.class));
	}

	@Test
	void keyToRefinery4Test() {
		var refineryTuple = MatcherUtils.keyToRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3), Tuple.of(5),
				Tuple.of(8), -99));
		assertThat(refineryTuple.getSize(), is(4));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple.get(2), is(5));
		assertThat(refineryTuple.get(3), is(8));
		assertThat(refineryTuple, instanceOf(Tuple4.class));
	}

	@Test
	void keyToRefinery5Test() {
		var refineryTuple = MatcherUtils.keyToRefineryTuple(Tuples.flatTupleOf(Tuple.of(2), Tuple.of(3), Tuple.of(5),
				Tuple.of(8), Tuple.of(13), -99));
		assertThat(refineryTuple.getSize(), is(5));
		assertThat(refineryTuple.get(0), is(2));
		assertThat(refineryTuple.get(1), is(3));
		assertThat(refineryTuple.get(2), is(5));
		assertThat(refineryTuple.get(3), is(8));
		assertThat(refineryTuple.get(4), is(13));
		assertThat(refineryTuple, instanceOf(TupleN.class));
	}

	@Test
	void keyToRefineryTooShortTest() {
		var viatraTuple = Tuples.flatTupleOf();
		assertThrows(IllegalArgumentException.class, () -> MatcherUtils.keyToRefineryTuple(viatraTuple));
	}

	@Test
	void keyToRefineryInvalidValueTest() {
		var viatraTuple = Tuples.flatTupleOf(Tuple.of(2), -98, -99);
		assertThrows(IllegalArgumentException.class, () -> MatcherUtils.keyToRefineryTuple(viatraTuple));
	}

	@Test
	void getSingleValueTest() {
		var value = MatcherUtils.getSingleValue(List.of(Tuples.flatTupleOf(Tuple.of(2), -99)));
		assertThat(value, is(-99));
	}

	// Static analysis accurately determines that the result is always {@code null}, but we check anyways.
	@SuppressWarnings("ConstantValue")
	@Test
	void getSingleValueNullTest() {
		var value = MatcherUtils.getSingleValue((Iterable<? extends ITuple>) null);
		assertThat(value, nullValue());
	}

	@Test
	void getSingleValueEmptyTest() {
		var value = MatcherUtils.getSingleValue(List.of());
		assertThat(value, nullValue());
	}

	@Test
	void getSingleValueMultipleTest() {
		var viatraTuples = List.of(Tuples.flatTupleOf(Tuple.of(2), -98), Tuples.flatTupleOf(Tuple.of(2), -99));
		assertThrows(IllegalStateException.class, () -> MatcherUtils.getSingleValue(viatraTuples));
	}
}
