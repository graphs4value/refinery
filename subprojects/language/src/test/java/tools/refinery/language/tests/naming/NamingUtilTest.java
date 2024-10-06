/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.naming;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.naming.NamingUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamingUtilTest {
	@ParameterizedTest
	@ValueSource(strings = {
			"a",
			"aaa",
			"a12",
			"a_b",
			"_",
			"_a",
			"'a'",
			"'11'",
			"'_a'",
			"'af dfd'",
			"'af ::% g'",
			"'af \\n\\' g'"
	})
	void validIdTest(String name) {
		assertTrue(NamingUtil.isValidId(name));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"1",
			"%",
			"1a",
			"a%",
			"a'b'",
			"'a' '",
			"'a",
			"'a\\",
			"'a'b",
			"a::b",
			"'a'::b"
	})
	void invalidIdTest(String name) {
		assertFalse(NamingUtil.isValidId(name));
	}
}
