/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.theory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static tools.refinery.store.reasoning.theory.TheorySupport.EXPLICIT_ONLY;
import static tools.refinery.store.reasoning.theory.TheorySupport.ENABLED_BY_DEFAULT;
import static tools.refinery.store.reasoning.theory.TheorySupport.UNSUPPORTED;
import static tools.refinery.store.reasoning.theory.TheorySupport.enabledByDefault;

class TheorySupportTest {
	@ParameterizedTest(name = "{0}.meet({1}) == {2}")
	@MethodSource
	void meetTest(TheorySupport a, TheorySupport b, TheorySupport expected) {
		assertThat(a.meet(b), equalTo(expected));
	}

	static Stream<Arguments> meetTest() {
		return Stream.of(
				// Combining two theories that are both enabled by default at the same preference keeps them
				// combined, so that recursively checking sub-expressions against multiple theories and meeting
				// the results together preserves the ability to route the whole expression to all of them.
				Arguments.of(ENABLED_BY_DEFAULT, ENABLED_BY_DEFAULT, ENABLED_BY_DEFAULT),
				// When preferences differ, the weaker (lower) preference wins, since it is the one that would
				// still let both theories be combined for the expression.
				Arguments.of(enabledByDefault(1), enabledByDefault(2), enabledByDefault(1)),
				Arguments.of(enabledByDefault(2), enabledByDefault(1), enabledByDefault(1)),
				// Meeting with an explicit-only theory downgrades a default preference to explicit-only, since
				// the sub-expression as a whole can no longer be routed there by default.
				Arguments.of(enabledByDefault(5), EXPLICIT_ONLY, EXPLICIT_ONLY),
				Arguments.of(EXPLICIT_ONLY, enabledByDefault(5), EXPLICIT_ONLY),
				Arguments.of(EXPLICIT_ONLY, EXPLICIT_ONLY, EXPLICIT_ONLY),
				// Meeting with an unsupported theory always yields unsupported, regardless of preference.
				Arguments.of(enabledByDefault(5), UNSUPPORTED, UNSUPPORTED),
				Arguments.of(UNSUPPORTED, enabledByDefault(5), UNSUPPORTED),
				Arguments.of(EXPLICIT_ONLY, UNSUPPORTED, UNSUPPORTED),
				Arguments.of(UNSUPPORTED, EXPLICIT_ONLY, UNSUPPORTED),
				Arguments.of(UNSUPPORTED, UNSUPPORTED, UNSUPPORTED)
		);
	}

	@ParameterizedTest(name = "{0}.isSupported() == {1}")
	@MethodSource
	void isSupportedTest(TheorySupport support, boolean expected) {
		assertThat(support.isSupported(), equalTo(expected));
	}

	static Stream<Arguments> isSupportedTest() {
		return Stream.of(
				Arguments.of(ENABLED_BY_DEFAULT, true),
				Arguments.of(enabledByDefault(42), true),
				Arguments.of(EXPLICIT_ONLY, true),
				Arguments.of(UNSUPPORTED, false)
		);
	}
}
