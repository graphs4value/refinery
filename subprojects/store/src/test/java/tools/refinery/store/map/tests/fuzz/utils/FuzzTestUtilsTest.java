/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.tests.fuzz.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

class FuzzTestUtilsTest {
	@Test
	void permutationInternalTest() {
		List<List<Object>> res = FuzzTestUtils.permutationInternal(0, new Object[]{1, 2, 3},
				new Object[]{'a', 'b', 'c'}, new Object[]{"alpha", "beta", "gamma", "delta"});
		assertEquals(3 * 3 * 4, res.size());
	}

	@Test
	void permutationTest1() {
		var res = FuzzTestUtils.permutation(new Object[]{1, 2, 3}, new Object[]{'a', 'b', 'c'},
				new Object[]{"alpha", "beta", "gamma", "delta"});
		assertEquals(3 * 3 * 4, res.count());
	}

	@Test
	void permutationTest2() {
		var res = FuzzTestUtils.permutation(new Object[]{1, 2, 3}, new Object[]{'a', 'b', 'c'},
				new Object[]{"alpha", "beta", "gamma", "delta"});
		Optional<Arguments> first = res.findFirst();
		assertTrue(first.isPresent());
		var arguments = first.get().get();
		assertEquals(1, arguments[0]);
		assertEquals('a', arguments[1]);
		assertEquals("alpha", arguments[2]);
	}
}
