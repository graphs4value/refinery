/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.logic.term.intinterval.Bound.Infinite.NEGATIVE_INFINITY;
import static tools.refinery.logic.term.intinterval.Bound.Infinite.POSITIVE_INFINITY;

class BoundTest {
	@ParameterizedTest
	@MethodSource
	void testAdd(Bound left, Bound right, Bound result) {
		assertThat(left.add(right, null), is(result));
	}

	static Stream<Arguments> testAdd() {
		return Stream.of(
				Arguments.of(Bound.of(2), Bound.of(3), Bound.of(5)),
				Arguments.of(Bound.of(2), POSITIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(Bound.of(2), NEGATIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, Bound.of(3), POSITIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, Bound.of(3), NEGATIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MAX_VALUE), Bound.of(Integer.MAX_VALUE), POSITIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MAX_VALUE), Bound.of(Integer.MIN_VALUE), Bound.of(-1)),
				Arguments.of(Bound.of(Integer.MIN_VALUE), Bound.of(Integer.MIN_VALUE), NEGATIVE_INFINITY)
		);
	}

	@ParameterizedTest
	@MethodSource
	void testAddSpecial(Bound left, Bound right, Bound.Infinite result) {
		assertThat(left.add(right, result), is(result));
	}

	static Stream<Arguments> testAddSpecial() {
		return Stream.of(
				Arguments.of(POSITIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY)
		);
	}

	@ParameterizedTest
	@MethodSource
	void testSub(Bound left, Bound right, Bound result) {
		assertThat(left.sub(right, null), is(result));
	}

	static Stream<Arguments> testSub() {
		return Stream.of(
				Arguments.of(Bound.of(2), Bound.of(3), Bound.of(-1)),
				Arguments.of(Bound.of(2), POSITIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(Bound.of(2), NEGATIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, Bound.of(3), POSITIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, Bound.of(3), NEGATIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MAX_VALUE), Bound.of(Integer.MIN_VALUE), POSITIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MIN_VALUE), Bound.of(-Integer.MAX_VALUE), Bound.of(-1)),
				Arguments.of(Bound.of(Integer.MIN_VALUE), Bound.of(Integer.MAX_VALUE), NEGATIVE_INFINITY)
		);
	}

	@ParameterizedTest
	@MethodSource
	void testSubSpecial(Bound left, Bound right, Bound.Infinite result) {
		assertThat(left.sub(right, result), is(result));
	}

	static Stream<Arguments> testSubSpecial() {
		return Stream.of(
				Arguments.of(POSITIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY)
		);
	}

	@ParameterizedTest
	@MethodSource
	void testMul(Bound left, Bound right, Bound result) {
		assertThat(left.mul(right), is(result));
	}

	static Stream<Arguments> testMul() {
		return Stream.of(
				Arguments.of(Bound.of(2), Bound.of(3), Bound.of(6)),
				Arguments.of(Bound.of(2), Bound.of(-3), Bound.of(-6)),
				Arguments.of(Bound.of(2), POSITIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(Bound.of(2), NEGATIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(Bound.of(0), POSITIVE_INFINITY, Bound.of(0)),
				Arguments.of(Bound.of(0), NEGATIVE_INFINITY, Bound.of(0)),
				Arguments.of(Bound.of(-2), POSITIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(Bound.of(-2), NEGATIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, Bound.of(3), POSITIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, Bound.of(0), Bound.of(0)),
				Arguments.of(POSITIVE_INFINITY, Bound.of(-3), NEGATIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(POSITIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, Bound.of(3), NEGATIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, Bound.of(0), Bound.of(0)),
				Arguments.of(NEGATIVE_INFINITY, Bound.of(-3), POSITIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY),
				Arguments.of(NEGATIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MAX_VALUE / 2), Bound.of(3), POSITIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MAX_VALUE / 2), Bound.of(-3), NEGATIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MIN_VALUE / 2), Bound.of(3), NEGATIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MIN_VALUE / 2), Bound.of(-3), POSITIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MAX_VALUE), Bound.of(1), Bound.of(Integer.MAX_VALUE)),
				Arguments.of(Bound.of(Integer.MAX_VALUE), Bound.of(-1), Bound.of(-Integer.MAX_VALUE)),
				Arguments.of(Bound.of(Integer.MAX_VALUE), Bound.of(Integer.MAX_VALUE), POSITIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MIN_VALUE), Bound.of(-1), POSITIVE_INFINITY),
				Arguments.of(Bound.of(Integer.MIN_VALUE), Bound.of(Integer.MIN_VALUE), POSITIVE_INFINITY)
		);
	}

	@ParameterizedTest
	@MethodSource
	void testCompareBound(Bound left, Bound right, int result) {
		assertThat(left.compareBound(right), is(result));
	}

	static Stream<Arguments> testCompareBound() {
		return Stream.of(
                Arguments.of(Bound.of(2), Bound.of(3), -1),
                Arguments.of(Bound.of(2), Bound.of(2), 0),
                Arguments.of(Bound.of(3), Bound.of(2), 1),
				Arguments.of(POSITIVE_INFINITY, Bound.of(2), -1),
				Arguments.of(POSITIVE_INFINITY, POSITIVE_INFINITY, 0),
				Arguments.of(POSITIVE_INFINITY, NEGATIVE_INFINITY, -1),
				Arguments.of(NEGATIVE_INFINITY, Bound.of(2), 1),
				Arguments.of(NEGATIVE_INFINITY, POSITIVE_INFINITY, 1),
				Arguments.of(NEGATIVE_INFINITY, NEGATIVE_INFINITY, 0)
        );
	}
}
