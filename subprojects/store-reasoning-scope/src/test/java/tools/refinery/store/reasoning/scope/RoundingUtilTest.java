/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RoundingUtilTest {
	@ParameterizedTest
	@MethodSource
	void roundUpTest(double value, int expected) {
		int actual = RoundingUtil.roundUp(value);
		assertThat(actual, is(expected));
	}

	static Stream<Arguments> roundUpTest() {
		return Stream.of(
				Arguments.of(0.0, 0),
				Arguments.of(-0.0, 0),
				Arguments.of(-0.9, 0),
				Arguments.of(-2, 0),
				Arguments.of(0.009, 0),
				Arguments.of(0.011, 1),
				Arguments.of(0.1, 1),
				Arguments.of(0.991, 1),
				Arguments.of(1, 1),
				Arguments.of(1.009, 1),
				Arguments.of(1.011, 2),
				Arguments.of(1.5, 2),
				Arguments.of(2, 2),
				Arguments.of(100.5, 101)
		);
	}

	@ParameterizedTest
	@MethodSource
	void roundDownTest(double value, int expected) {
		int actual = RoundingUtil.roundDown(value);
		assertThat(actual, is(expected));
	}

	static Stream<Arguments> roundDownTest() {
		return Stream.of(
				Arguments.of(0.0, 0),
				Arguments.of(-0.0, 0),
				Arguments.of(-0.9, 0),
				Arguments.of(-2, 0),
				Arguments.of(0.989, 0),
				Arguments.of(0.991, 1),
				Arguments.of(1, 1),
				Arguments.of(1.5, 1),
				Arguments.of(1.009, 1),
				Arguments.of(1.989, 1),
				Arguments.of(1.991, 2),
				Arguments.of(2, 2),
				Arguments.of(2.009, 2),
				Arguments.of(100.5, 100)
		);
	}
}
