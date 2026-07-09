/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex;

import org.junit.jupiter.api.Test;
import tools.refinery.ibex.Ibex;
import tools.refinery.ibex.IbexSolverLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IbexReasoningTest {
	private static final double EPS = 1e-3;

	@Test
	void reasoningTest() {
		assertDoesNotThrow(IbexSolverLoader::loadNativeLibraries);
		var ibex = new Ibex(new double[]{1e-2, -1}, true);
		try {
			ibex.add_ctr("{0}-{1}=0");
			ibex.build();
			var domains = new double[]{1.5, 10.5, 5.5, 12.0};
			int result = ibex.contract(0, domains, EPS);
			assertEquals(Ibex.CONTRACT, result);
			assertEquals(5.5, domains[0], EPS);
			assertEquals(10.5, domains[1], EPS);
			assertEquals(5.5, domains[2], EPS);
			assertEquals(10.5, domains[3], EPS);
		} finally {
			ibex.release();
		}
	}
}
