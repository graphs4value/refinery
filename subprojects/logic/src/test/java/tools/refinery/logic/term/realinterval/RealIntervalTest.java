/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static tools.refinery.logic.term.realinterval.RealBound.Infinite.NEGATIVE_INFINITY;
import static tools.refinery.logic.term.realinterval.RealBound.Infinite.POSITIVE_INFINITY;

class RealIntervalTest {
	@ParameterizedTest(name = "{0} / {1} == {2}")
	@MethodSource
	void divTest(RealInterval a, RealInterval b, RealInterval expected) {
		var actual = a.div(b);
		assertThat(actual, equalTo(expected));
	}

	static Stream<Arguments> divTest() {
		return Stream.of(
				// Basic positive division
				Arguments.of(RealInterval.of("1", "2"), RealInterval.of("2"), RealInterval.of("0.5", "1")),
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of("2"),
						RealInterval.of("0.5", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "2"), RealInterval.of("2"),
						RealInterval.of(NEGATIVE_INFINITY, "1")),

				// Division by negative numbers
				Arguments.of(RealInterval.of("-2", "-1"), RealInterval.of("-2"), RealInterval.of("0.5", "1")),
				Arguments.of(RealInterval.of("1", "2"), RealInterval.of("-4", "-2"), RealInterval.of("-1", "-0.25")),
				Arguments.of(RealInterval.of("-2", "-1"), RealInterval.of("-4", "-2"), RealInterval.of("0.25", "1")),

				// Negative dividends
				Arguments.of(RealInterval.of("-2", "-1"), RealInterval.of("2"), RealInterval.of("-1", "-0.5")),
				Arguments.of(RealInterval.of("-2", "-1"), RealInterval.of("1", "2"), RealInterval.of("-2", "-0.5")),

				// Intervals spanning zero (ZERO_PROPER)
				Arguments.of(RealInterval.of("-1", "1"), RealInterval.of("2"), RealInterval.of("-0.5", "0.5")),
				Arguments.of(RealInterval.of("-2", "3"), RealInterval.of("1", "4"), RealInterval.of("-2", "3")),

				// Division by intervals containing zero
				Arguments.of(RealInterval.of("1", "2"), RealInterval.ZERO, RealInterval.ERROR),
				Arguments.of(RealInterval.of("1", "2"), RealInterval.of("-1", "1"), RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of("1", "2"), RealInterval.of("-1", "0"),
						RealInterval.of(NEGATIVE_INFINITY, "-1")),
				Arguments.of(RealInterval.of("1", "2"), RealInterval.of("0", "1"),
						RealInterval.of("1", POSITIVE_INFINITY)),

				// Division of zero
				Arguments.of(RealInterval.ZERO, RealInterval.of("2"), RealInterval.ZERO),
				Arguments.of(RealInterval.ZERO, RealInterval.of("-1", "1"), RealInterval.ZERO),

				// Positive infinity dividend
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of("2", POSITIVE_INFINITY),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of("2", POSITIVE_INFINITY), RealInterval.of("1", POSITIVE_INFINITY),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of(NEGATIVE_INFINITY, "-1"),
						RealInterval.of(NEGATIVE_INFINITY, "0")),

				// Negative infinity dividend
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "-1"), RealInterval.of(NEGATIVE_INFINITY, "-2"),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "-2"), RealInterval.of(NEGATIVE_INFINITY, "-1"),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "-1"), RealInterval.of("1", POSITIVE_INFINITY),
						RealInterval.of(NEGATIVE_INFINITY, "0")),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "-1"), RealInterval.of("2"),
						RealInterval.of(NEGATIVE_INFINITY, "-0.5")),

				// Full range (UNKNOWN) dividend
				Arguments.of(RealInterval.UNKNOWN, RealInterval.of("2"), RealInterval.UNKNOWN),
				Arguments.of(RealInterval.UNKNOWN, RealInterval.of("1", POSITIVE_INFINITY), RealInterval.UNKNOWN),
				Arguments.of(RealInterval.UNKNOWN, RealInterval.of(NEGATIVE_INFINITY, "-1"), RealInterval.UNKNOWN),

				// Infinity with zero-spanning divisor
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of("-1", "1"),
						RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "-1"), RealInterval.of("-1", "1"),
						RealInterval.UNKNOWN),

				// One-sided infinity combinations
				Arguments.of(RealInterval.of("0", POSITIVE_INFINITY), RealInterval.of("0", POSITIVE_INFINITY),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of("-1", POSITIVE_INFINITY),
						RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of("2", POSITIVE_INFINITY),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of("-1", POSITIVE_INFINITY), RealInterval.of("-1", POSITIVE_INFINITY),
						RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of("-1", POSITIVE_INFINITY), RealInterval.of("2", POSITIVE_INFINITY),
						RealInterval.of("-0.5", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "0"), RealInterval.of(NEGATIVE_INFINITY, "0"),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "1"), RealInterval.of(NEGATIVE_INFINITY, "-2"),
						RealInterval.of("-0.5", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "1"), RealInterval.of(NEGATIVE_INFINITY, "2"),
						RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "-1"), RealInterval.of(NEGATIVE_INFINITY, "-2"),
						RealInterval.of("0", POSITIVE_INFINITY)),
				Arguments.of(RealInterval.of(NEGATIVE_INFINITY, "-1"), RealInterval.of(NEGATIVE_INFINITY, "2"),
						RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of("0", POSITIVE_INFINITY), RealInterval.of(NEGATIVE_INFINITY, "0"),
						RealInterval.of(NEGATIVE_INFINITY, "0")),
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of(NEGATIVE_INFINITY, "2"),
						RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of("1", POSITIVE_INFINITY), RealInterval.of(NEGATIVE_INFINITY, "-2"),
						RealInterval.of(NEGATIVE_INFINITY, "0")),
				Arguments.of(RealInterval.of("-1", POSITIVE_INFINITY), RealInterval.of(NEGATIVE_INFINITY, "2"),
						RealInterval.UNKNOWN),
				Arguments.of(RealInterval.of("-1", POSITIVE_INFINITY), RealInterval.of(NEGATIVE_INFINITY, "-2"),
						RealInterval.of(NEGATIVE_INFINITY, "0.5")),

				// Pure infinity cases
				Arguments.of(RealInterval.POSITIVE_INFINITY, RealInterval.POSITIVE_INFINITY,
						RealInterval.POSITIVE_INFINITY),
				Arguments.of(RealInterval.NEGATIVE_INFINITY, RealInterval.NEGATIVE_INFINITY,
						RealInterval.POSITIVE_INFINITY),
				Arguments.of(RealInterval.POSITIVE_INFINITY, RealInterval.NEGATIVE_INFINITY,
						RealInterval.NEGATIVE_INFINITY),
				Arguments.of(RealInterval.NEGATIVE_INFINITY, RealInterval.POSITIVE_INFINITY,
						RealInterval.NEGATIVE_INFINITY),

				// Verify that operations on empty intervals preserve sign information
				Arguments.of(RealInterval.POSITIVE_INFINITY, RealInterval.of("2"),
						RealInterval.POSITIVE_INFINITY),
				Arguments.of(RealInterval.POSITIVE_INFINITY, RealInterval.of("-2"),
						RealInterval.NEGATIVE_INFINITY),
				Arguments.of(RealInterval.NEGATIVE_INFINITY, RealInterval.of("2"),
						RealInterval.NEGATIVE_INFINITY),
				Arguments.of(RealInterval.NEGATIVE_INFINITY, RealInterval.of("-2"),
						RealInterval.POSITIVE_INFINITY),

				// Division of finite values by infinity
				Arguments.of(RealInterval.of("1", "2"), RealInterval.POSITIVE_INFINITY,
						RealInterval.ZERO),
				Arguments.of(RealInterval.of("1", "2"), RealInterval.NEGATIVE_INFINITY,
						RealInterval.ZERO),
				Arguments.of(RealInterval.of("-2", "-1"), RealInterval.POSITIVE_INFINITY,
						RealInterval.ZERO),
				Arguments.of(RealInterval.of("-2", "-1"), RealInterval.NEGATIVE_INFINITY,
						RealInterval.ZERO),

				// ERROR cases
				Arguments.of(RealInterval.ERROR, RealInterval.of("2"), RealInterval.ERROR),
				Arguments.of(RealInterval.of("2"), RealInterval.ERROR, RealInterval.ERROR),
				Arguments.of(RealInterval.POSITIVE_INFINITY, RealInterval.ERROR,
						RealInterval.ERROR),
				Arguments.of(RealInterval.NEGATIVE_INFINITY, RealInterval.ERROR,
						RealInterval.ERROR),

				// Improper interval division (paraconsistent cases)
				// Both improper - result should also be improper
				Arguments.of(RealInterval.of("4", "2"), RealInterval.of("2", "1"),
						RealInterval.of("4", "1")),

				// One improper, one proper
				Arguments.of(RealInterval.of("4", "2"), RealInterval.of("1", "2"),
						RealInterval.of("2")),
				Arguments.of(RealInterval.of("2", "4"), RealInterval.of("2", "1"),
						RealInterval.of("2")),

				// Improper with negative values
				Arguments.of(RealInterval.of("2", "-2"), RealInterval.of("1", "2"),
						RealInterval.of("1", "-1")),
				Arguments.of(RealInterval.of("-2", "-4"), RealInterval.of("2", "1"),
						RealInterval.of("-1", "-4")),

				// Improper error
				Arguments.of(RealInterval.of("4", "2"), RealInterval.of("2", "-1"), RealInterval.ERROR)
		);
	}
}
