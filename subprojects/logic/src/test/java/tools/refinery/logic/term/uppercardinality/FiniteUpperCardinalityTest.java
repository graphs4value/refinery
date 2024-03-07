/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.uppercardinality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FiniteUpperCardinalityTest {
	@Test
	void invalidConstructorTest() {
		assertThrows(IllegalArgumentException.class, () -> new FiniteUpperCardinality(-1));
	}
}
