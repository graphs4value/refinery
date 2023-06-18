/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.query.term.real.RealTerms;
import tools.refinery.store.query.valuation.Valuation;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class IntTermsEvaluateTest {
	@ParameterizedTest(name = "+{0} == {1}")
	@CsvSource(value = {
			"2, 2",
			"null, null"
	}, nullValues = "null")
	void plusTest(Integer a, Integer result) {
		var term = IntTerms.plus(IntTerms.constant(a));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "-{0} == {1}")
	@CsvSource(value = {
			"2, -2",
			"null, null"
	}, nullValues = "null")
	void minusTest(Integer a, Integer result) {
		var term = IntTerms.minus(IntTerms.constant(a));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} + {1} == {2}")
	@CsvSource(value = {
			"1, 2, 3",
			"null, 2, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void addTest(Integer a, Integer b, Integer result) {
		var term = IntTerms.add(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} - {1} == {2}")
	@CsvSource(value = {
			"1, 3, -2",
			"null, 3, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void subTest(Integer a, Integer b, Integer result) {
		var term = IntTerms.sub(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} * {1} == {2}")
	@CsvSource(value = {
			"2, 3, 6",
			"null, 3, null",
			"2, null, null",
			"null, null, null"
	}, nullValues = "null")
	void mulTest(Integer a, Integer b, Integer result) {
		var term = IntTerms.mul(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} * {1} == {2}")
	@CsvSource(value = {
			"6, 3, 2",
			"7, 3, 2",
			"6, 0, null",
			"null, 3, null",
			"6, null, null",
			"null, null, null"
	}, nullValues = "null")
	void divTest(Integer a, Integer b, Integer result) {
		var term = IntTerms.div(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} ** {1} == {2}")
	@CsvSource(value = {
			"1, 0, 1",
			"1, 3, 1",
			"1, -3, null",
			"2, 0, 1",
			"2, 2, 4",
			"2, 3, 8",
			"2, 4, 16",
			"2, 5, 32",
			"2, 6, 64",
			"2, -3, null",
			"null, 3, null",
			"2, null, null",
			"null, null, null"
	}, nullValues = "null")
	void powTest(Integer a, Integer b, Integer result) {
		var term = IntTerms.pow(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "min({0}, {1}) == {2}")
	@CsvSource(value = {
			"1, 2, 1",
			"2, 1, 1",
			"null, 2, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void minTest(Integer a, Integer b, Integer result) {
		var term = IntTerms.min(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "max({0}, {1}) == {2}")
	@CsvSource(value = {
			"1, 2, 2",
			"2, 1, 2",
			"null, 2, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void maxTest(Integer a, Integer b, Integer result) {
		var term = IntTerms.max(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} == {1}) == {2}")
	@CsvSource(value = {
			"1, 1, true",
			"1, 2, false",
			"null, 1, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void eqTest(Integer a, Integer b, Boolean result) {
		var term = IntTerms.eq(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} != {1}) == {2}")
	@CsvSource(value = {
			"1, 1, false",
			"1, 2, true",
			"null, 1, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void notEqTest(Integer a, Integer b, Boolean result) {
		var term = IntTerms.notEq(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} < {1}) == {2}")
	@CsvSource(value = {
			"1, -2, false",
			"1, 1, false",
			"1, 2, true",
			"null, 1, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void lessTest(Integer a, Integer b, Boolean result) {
		var term = IntTerms.less(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} <= {1}) == {2}")
	@CsvSource(value = {
			"1, -2, false",
			"1, 1, true",
			"1, 2, true",
			"null, 1, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void lessEqTest(Integer a, Integer b, Boolean result) {
		var term = IntTerms.lessEq(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} > {1}) == {2}")
	@CsvSource(value = {
			"1, -2, true",
			"1, 1, false",
			"1, 2, false",
			"null, 1, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void greaterTest(Integer a, Integer b, Boolean result) {
		var term = IntTerms.greater(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} >= {1}) == {2}")
	@CsvSource(value = {
			"1, -2, true",
			"1, 1, true",
			"1, 2, false",
			"null, 1, null",
			"1, null, null",
			"null, null, null"
	}, nullValues = "null")
	void greaterEqTest(Integer a, Integer b, Boolean result) {
		var term = IntTerms.greaterEq(IntTerms.constant(a), IntTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} as int == {1}")
	@MethodSource
	void asIntTest(Double a, Integer result) {
		var term = IntTerms.asInt(RealTerms.constant(a));
		assertThat(term.getType(), is(Integer.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	static Stream<Arguments> asIntTest() {
		return Stream.of(
				Arguments.of(2.0, 2),
				Arguments.of(2.1, 2),
				Arguments.of(2.9, 2),
				Arguments.of(-2.0, -2),
				Arguments.of(-2.1, -2),
				Arguments.of(-2.9, -2),
				Arguments.of(0.0, 0),
				Arguments.of(-0.0, 0),
				Arguments.of(Double.POSITIVE_INFINITY, Integer.MAX_VALUE),
				Arguments.of(Double.NEGATIVE_INFINITY, Integer.MIN_VALUE),
				Arguments.of(Double.NaN, null),
				Arguments.of(null, null)
		);
	}
}
