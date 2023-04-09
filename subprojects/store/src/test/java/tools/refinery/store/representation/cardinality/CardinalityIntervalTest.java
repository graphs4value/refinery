/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.representation.cardinality.CardinalityInterval;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static tools.refinery.store.representation.cardinality.CardinalityIntervals.*;

class CardinalityIntervalTest {
	@ParameterizedTest(name = "min({0}, {1}) == {2}")
	@MethodSource
	void minTest(CardinalityInterval a, CardinalityInterval b, CardinalityInterval expected) {
		assertThat(a.min(b), equalTo(expected));
	}

	static Stream<Arguments> minTest() {
		return Stream.of(
				Arguments.of(atMost(1), atMost(1), atMost(1)),
				Arguments.of(atMost(1), between(2, 3), atMost(1)),
				Arguments.of(atMost(1), atLeast(2), atMost(1)),
				Arguments.of(atMost(1), ERROR, ERROR),
				Arguments.of(atLeast(1), atLeast(2), atLeast(1)),
				Arguments.of(atLeast(1), ERROR, ERROR),
				Arguments.of(ERROR, atLeast(2), ERROR),
				Arguments.of(ERROR, ERROR, ERROR)
		);
	}

	@ParameterizedTest(name = "max({0}, {1}) == {2}")
	@MethodSource
	void maxTest(CardinalityInterval a, CardinalityInterval b, CardinalityInterval expected) {
		assertThat(a.max(b), equalTo(expected));
	}

	static Stream<Arguments> maxTest() {
		return Stream.of(
				Arguments.of(atMost(1), atMost(1), atMost(1)),
				Arguments.of(atMost(1), between(2, 3), between(2, 3)),
				Arguments.of(atMost(1), atLeast(2), atLeast(2)),
				Arguments.of(atMost(1), ERROR, ERROR),
				Arguments.of(atLeast(1), atLeast(2), atLeast(2)),
				Arguments.of(atLeast(1), ERROR, ERROR),
				Arguments.of(ERROR, atLeast(2), ERROR),
				Arguments.of(ERROR, ERROR, ERROR)
		);
	}

	@ParameterizedTest(name = "{0} + {1} == {2}")
	@MethodSource
	void addTest(CardinalityInterval a, CardinalityInterval b, CardinalityInterval expected) {
		assertThat(a.add(b), equalTo(expected));
	}

	static Stream<Arguments> addTest() {
		return Stream.of(
				Arguments.of(atMost(1), atMost(1), atMost(2)),
				Arguments.of(atMost(1), between(2, 3), between(2, 4)),
				Arguments.of(atMost(1), atLeast(2), atLeast(2)),
				Arguments.of(atMost(1), ERROR, ERROR),
				Arguments.of(atLeast(1), atLeast(2), atLeast(3)),
				Arguments.of(atLeast(1), ERROR, ERROR),
				Arguments.of(ERROR, atLeast(2), ERROR),
				Arguments.of(ERROR, ERROR, ERROR)
		);
	}

	@ParameterizedTest(name = "{0} * {1} == {2}")
	@MethodSource
	void multiplyTest(CardinalityInterval a, CardinalityInterval b, CardinalityInterval expected) {
		assertThat(a.multiply(b), equalTo(expected));
	}

	static Stream<Arguments> multiplyTest() {
		return Stream.of(
				Arguments.of(between(2, 3), between(4, 5), between(8, 15)),
				Arguments.of(atLeast(2), between(4, 5), atLeast(8)),
				Arguments.of(between(2, 3), atLeast(4), atLeast(8)),
				Arguments.of(between(2, 3), ERROR, ERROR),
				Arguments.of(ERROR, between(4, 5), ERROR),
				Arguments.of(ERROR, ERROR, ERROR)
		);
	}

	@ParameterizedTest(name = "{0} /\\ {1} == {2}")
	@MethodSource
	void meetTest(CardinalityInterval a, CardinalityInterval b, CardinalityInterval expected) {
		assertThat(a.meet(b), equalTo(expected));
	}

	static Stream<Arguments> meetTest() {
		return Stream.of(
				Arguments.of(atMost(1), atMost(2), atMost(1)),
				Arguments.of(atMost(2), between(1, 3), between(1, 2)),
				Arguments.of(atMost(1), between(1, 3), exactly(1)),
				Arguments.of(atMost(1), between(2, 3), ERROR),
				Arguments.of(atMost(1), ERROR, ERROR),
				Arguments.of(ERROR, atMost(1), ERROR),
				Arguments.of(ERROR, ERROR, ERROR)
		);
	}

	@ParameterizedTest(name = "{0} \\/ {1} == {2}")
	@MethodSource
	void joinTest(CardinalityInterval a, CardinalityInterval b, CardinalityInterval expected) {
		assertThat(a.join(b), equalTo(expected));
	}

	static Stream<Arguments> joinTest() {
		return Stream.of(
				Arguments.of(atMost(1), atMost(2), atMost(2)),
				Arguments.of(atMost(2), between(1, 3), atMost(3)),
				Arguments.of(atMost(1), between(2, 3), atMost(3)),
				Arguments.of(atMost(1), ERROR, atMost(1)),
				Arguments.of(ERROR, atMost(1), atMost(1)),
				Arguments.of(ERROR, ERROR, ERROR)
		);
	}
}
