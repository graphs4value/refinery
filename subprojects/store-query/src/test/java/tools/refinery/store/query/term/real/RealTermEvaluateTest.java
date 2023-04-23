/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.real;

import org.hamcrest.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.refinery.store.query.term.int_.IntTerms;
import tools.refinery.store.query.valuation.Valuation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RealTermEvaluateTest {
	public static final double TOLERANCE = 1e-6;

	private static Matcher<Double> closeToOrNull(Double expected) {
		return expected == null ? nullValue(Double.class) : closeTo(expected, TOLERANCE);
	}

	@ParameterizedTest(name = "+{0} == {1}")
	@CsvSource(value = {
			"2.5, 2.5",
			"null, null"
	}, nullValues = "null")
	void plusTest(Double a, Double result) {
		var term = RealTerms.plus(RealTerms.constant(a));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "-{0} == {1}")
	@CsvSource(value = {
			"2.5, -2.5",
			"null, null"
	}, nullValues = "null")
	void minusTest(Double a, Double result) {
		var term = RealTerms.minus(RealTerms.constant(a));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "{0} + {1} == {2}")
	@CsvSource(value = {
			"1.2, 2.3, 3.5",
			"null, 2.3, null",
			"1.2, null, null",
			"null, null, null"
	}, nullValues = "null")
	void addTest(Double a, Double b, Double result) {
		var term = RealTerms.add(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "{0} - {1} == {2}")
	@CsvSource(value = {
			"1.2, 3.4, -2.2",
			"null, 3.4, null",
			"1.2, null, null",
			"null, null, null"
	}, nullValues = "null")
	void subTest(Double a, Double b, Double result) {
		var term = RealTerms.sub(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "{0} * {1} == {2}")
	@CsvSource(value = {
			"2.3, 3.4, 7.82",
			"null, 3.4, null",
			"2.3, null, null",
			"null, null, null"
	}, nullValues = "null")
	void mulTest(Double a, Double b, Double result) {
		var term = RealTerms.mul(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "{0} * {1} == {2}")
	@CsvSource(value = {
			"7.82, 3.4, 2.3",
			"null, 3.4, null",
			"7.82, null, null",
			"null, null, null"
	}, nullValues = "null")
	void divTest(Double a, Double b, Double result) {
		var term = RealTerms.div(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "{0} ** {1} == {2}")
	@CsvSource(value = {
			"2.0, 6.0, 64.0",
			"null, 6.0, null",
			"2.0, null, null",
			"null, null, null"
	}, nullValues = "null")
	void powTest(Double a, Double b, Double result) {
		var term = RealTerms.pow(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "min({0}, {1}) == {2}")
	@CsvSource(value = {
			"1.5, 2.7, 1.5",
			"2.7, 1.5, 1.5",
			"null, 2.7, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void minTest(Double a, Double b, Double result) {
		var term = RealTerms.min(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "max({0}, {1}) == {2}")
	@CsvSource(value = {
			"1.5, 2.7, 2.7",
			"2.7, 1.7, 2.7",
			"null, 2.7, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void maxTest(Double a, Double b, Double result) {
		var term = RealTerms.max(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}

	@ParameterizedTest(name = "({0} == {1}) == {2}")
	@CsvSource(value = {
			"1.5, 1.5, true",
			"1.5, 2.7, false",
			"null, 1.5, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void eqTest(Double a, Double b, Boolean result) {
		var term = RealTerms.eq(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} != {1}) == {2}")
	@CsvSource(value = {
			"1.5, 1.5, false",
			"1.5, 2.7, true",
			"null, 1.5, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void notEqTest(Double a, Double b, Boolean result) {
		var term = RealTerms.notEq(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} < {1}) == {2}")
	@CsvSource(value = {
			"1.5, -2.7, false",
			"1.5, 1.5, false",
			"1.5, 2.7, true",
			"null, 1.5, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void lessTest(Double a, Double b, Boolean result) {
		var term = RealTerms.less(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} <= {1}) == {2}")
	@CsvSource(value = {
			"1.5, -2.7, false",
			"1.5, 1.5, true",
			"1.5, 2.7, true",
			"null, 1.5, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void lessEqTest(Double a, Double b, Boolean result) {
		var term = RealTerms.lessEq(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} > {1}) == {2}")
	@CsvSource(value = {
			"1.5, -2.7, true",
			"1.5, 1.5, false",
			"1.5, 2.7, false",
			"null, 1.5, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void greaterTest(Double a, Double b, Boolean result) {
		var term = RealTerms.greater(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "({0} >= {1}) == {2}")
	@CsvSource(value = {
			"1.5, -2.7, true",
			"1.5, 1.5, true",
			"1.5, 2.7, false",
			"null, 1.5, null",
			"1.5, null, null",
			"null, null, null"
	}, nullValues = "null")
	void greaterEqTest(Double a, Double b, Boolean result) {
		var term = RealTerms.greaterEq(RealTerms.constant(a), RealTerms.constant(b));
		assertThat(term.getType(), is(Boolean.class));
		assertThat(term.evaluate(Valuation.empty()), is(result));
	}

	@ParameterizedTest(name = "{0} as real == {1}")
	@CsvSource(value = {
			"0, 0.0",
			"5, 5.0",
			"null, null"
	}, nullValues = "null")
	void asRealTest(Integer a, Double result) {
		var term = RealTerms.asReal(IntTerms.constant(a));
		assertThat(term.getType(), is(Double.class));
		assertThat(term.evaluate(Valuation.empty()), closeToOrNull(result));
	}
}
