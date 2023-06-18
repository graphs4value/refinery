/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class UpperCardinalityTest {
	@ParameterizedTest(name = "min({0}, {1}) == {2}")
	@MethodSource
	void minTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		assertThat(a.min(b), equalTo(expected));
	}

	static Stream<Arguments> minTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(1), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(1), UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(0), UpperCardinalities.UNBOUNDED, UpperCardinality.of(0)),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED)
		);
	}

	@ParameterizedTest(name = "max({0}, {1}) == {2}")
	@MethodSource
	void maxTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		assertThat(a.max(b), equalTo(expected));
	}

	static Stream<Arguments> maxTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(0), UpperCardinality.of(0)),
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(1), UpperCardinality.of(1)),
				Arguments.of(UpperCardinality.of(1), UpperCardinality.of(0), UpperCardinality.of(1)),
				Arguments.of(UpperCardinality.of(0), UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(0), UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED)
		);
	}

	@ParameterizedTest(name = "{0} + {1} == {2}")
	@MethodSource
	void addTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		assertThat(a.add(b), equalTo(expected));
	}

	static Stream<Arguments> addTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(2), UpperCardinality.of(3), UpperCardinality.of(5)),
				Arguments.of(UpperCardinality.of(2), UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(2), UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED)
		);
	}

	@ParameterizedTest(name = "{0} * {1} == {2}")
	@MethodSource
	void multiplyTest(UpperCardinality a, UpperCardinality b, UpperCardinality expected) {
		assertThat(a.multiply(b), equalTo(expected));
	}

	static Stream<Arguments> multiplyTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(2), UpperCardinality.of(3), UpperCardinality.of(6)),
				Arguments.of(UpperCardinality.of(2), UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(2), UpperCardinalities.UNBOUNDED),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED)
		);
	}

	@ParameterizedTest(name = "{0}.compareTo({1}) == {2}")
	@MethodSource
	void compareToTest(UpperCardinality a, UpperCardinality b, int expected) {
		assertThat(a.compareTo(b), equalTo(expected));
	}

	static Stream<Arguments> compareToTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(0), 0),
				Arguments.of(UpperCardinality.of(0), UpperCardinality.of(1), -1),
				Arguments.of(UpperCardinality.of(1), UpperCardinality.of(0), 1),
				Arguments.of(UpperCardinality.of(0), UpperCardinalities.UNBOUNDED, -1),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinality.of(0), 1),
				Arguments.of(UpperCardinalities.UNBOUNDED, UpperCardinalities.UNBOUNDED, 0)
		);
	}

	@ParameterizedTest(name = "{0}.compareToInt({1}) == {2}")
	@MethodSource
	void compareToIntTest(UpperCardinality a, int b, int expected) {
		assertThat(a.compareToInt(b), equalTo(expected));
	}

	static Stream<Arguments> compareToIntTest() {
		return Stream.of(
				Arguments.of(UpperCardinality.of(3), -1, 1),
				Arguments.of(UpperCardinality.of(3), 2, 1),
				Arguments.of(UpperCardinality.of(3), 3, 0),
				Arguments.of(UpperCardinality.of(3), 4, -1),
				Arguments.of(UpperCardinalities.UNBOUNDED, -1, 1),
				Arguments.of(UpperCardinalities.UNBOUNDED, 3, 1)
		);
	}
}
