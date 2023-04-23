/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.uppercardinality;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.query.valuation.Valuation;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class UpperCardinalityTermsEvaluateTest {
	@ParameterizedTest(name = "min({0}, {1}) == {2}")
	@MethodSource
	void minTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		var term = UpperCardinalityTerms.min(UpperCardinalityTerms.constant(a), UpperCardinalityTerms.constant(b));
		assertThat(term.getType(), is(UpperCardinality.class));
		assertThat(term.evaluate(Valuation.empty()), is(expected));
	}

	static Stream<Arguments> minTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(1), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(1), UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(0), UpperCardinalities.UNBOUNDED, UpperCardinality.of(0)),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinality.of(1), null, null),
				Arguments.of(null, UpperCardinality.of(1), null),
				Arguments.of(null, null, null)
		);
	}

	@ParameterizedTest(name = "max({0}, {1}) == {2}")
	@MethodSource
	void maxTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		var term = UpperCardinalityTerms.max(UpperCardinalityTerms.constant(a), UpperCardinalityTerms.constant(b));
		assertThat(term.getType(), is(UpperCardinality.class));
		assertThat(term.evaluate(Valuation.empty()), is(expected));
	}

	static Stream<Arguments> maxTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(1), UpperCardinality.of(1)),
				Arguments.of(UpperCardinality.of(1), UpperCardinality.of(0), UpperCardinality.of(1)),
				Arguments.of(UpperCardinality.of(0), UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(0), UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinality.of(1), null, null),
				Arguments.of(null, UpperCardinality.of(1), null),
				Arguments.of(null, null, null)
		);
	}

	@ParameterizedTest(name = "{0} + {1} == {2}")
	@MethodSource
	void addTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		var term = UpperCardinalityTerms.add(UpperCardinalityTerms.constant(a), UpperCardinalityTerms.constant(b));
		assertThat(term.getType(), is(UpperCardinality.class));
		assertThat(term.evaluate(Valuation.empty()), is(expected));
	}

	static Stream<Arguments> addTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(2), UpperCardinality.of(3), UpperCardinality.of(5)),
				Arguments.of(UpperCardinality.of(2), UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(2), UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinality.of(1), null, null),
				Arguments.of(null, UpperCardinality.of(1), null),
				Arguments.of(null, null, null)
		);
	}

	@ParameterizedTest(name = "{0} * {1} == {2}")
	@MethodSource
	void mulTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		var term = UpperCardinalityTerms.mul(UpperCardinalityTerms.constant(a), UpperCardinalityTerms.constant(b));
		assertThat(term.getType(), is(UpperCardinality.class));
		assertThat(term.evaluate(Valuation.empty()), is(expected));
	}

	static Stream<Arguments> mulTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(2), UpperCardinality.of(3), UpperCardinality.of(6)),
				Arguments.of(UpperCardinality.of(2), UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(2), UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinality.of(1), null, null),
				Arguments.of(null, UpperCardinality.of(1), null),
				Arguments.of(null, null, null)
		);
	}
}
