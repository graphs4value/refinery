/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.tests;

import org.junit.Test;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Direction;
import tools.refinery.interpreter.matchers.util.Signed;
import tools.refinery.interpreter.matchers.util.TimelyMemory;

import java.util.stream.StreamSupport;

/**
 * Tests for {@link TimelyMemory}.
 *
 * @author Tamas Szabo
 *
 */
public class LazyTimelyMemoryTest {

	private static final Tuple T = Tuples.staticArityFlatTupleOf("test");

	@Test
	public void testSinglePlateau() {
		final TimelyMemory<Integer> memory = createMemory();
		memory.put(T, 1);
		assertDiff(memory.resumeAt(1).get(T), +1);
		memory.remove(T, 4);
		assertDiff(memory.resumeAt(4).get(T), -4);
	}

	@Test
	public void testSinglePlateauBuildUpBreakDown() {
		final TimelyMemory<Integer> memory = createMemory();

		// build up the plateau
		memory.put(T, 1);
		assertDiff(memory.resumeAt(1).get(T), +1);
		memory.remove(T, 4);
		assertDiff(memory.resumeAt(4).get(T), -4);

		// break down the plateau
		memory.remove(T, 1);
		assertDiff(memory.resumeAt(1).get(T), -1, +4);

		memory.put(T, 4);
		assertDiff(memory.resumeAt(4).get(T));
	}

	@Test
	public void testSinglePlateauAndUpEdge() {
		final TimelyMemory<Integer> memory = createMemory();

		memory.put(T, 1);
		assertDiff(memory.resumeAt(1).get(T), +1);
		memory.remove(T, 4);
		assertDiff(memory.resumeAt(4).get(T), -4);
		memory.put(T, 8);
		assertDiff(memory.resumeAt(8).get(T), +8);

		memory.remove(T, 1);
		assertDiff(memory.resumeAt(1).get(T), -1, +4);
		memory.put(T, 4);
		assertDiff(memory.resumeAt(4).get(T));

		assertDiff(memory.get(T).asChangeSequence(), +8);
	}

	@Test
	public void testComplexPlateaus() {
		final TimelyMemory<Integer> memory = createMemory();

		memory.put(T, 1);
		assertDiff(memory.resumeAt(1).get(T), +1);
		memory.put(T, 4);
		assertDiff(memory.resumeAt(4).get(T));
		memory.put(T, 8);
		assertDiff(memory.resumeAt(8).get(T));
		memory.remove(T, 10);
		memory.remove(T, 10);
		memory.remove(T, 10);
		assertDiff(memory.resumeAt(10).get(T), -10);
		memory.put(T, 12);
		assertDiff(memory.resumeAt(12).get(T), +12);
		memory.remove(T, 14);
		assertDiff(memory.resumeAt(14).get(T), -14);

		assertDiff(memory.get(T).asChangeSequence(), +1, -10, +12, -14);

		memory.remove(T, 1);
		assertDiff(memory.resumeAt(1).get(T), -1, +4);

		memory.put(T, 2);
		assertDiff(memory.resumeAt(2).get(T), +2, -4);

		memory.remove(T, 4);
		assertDiff(memory.resumeAt(4).get(T));

		memory.remove(T, 8);
		assertDiff(memory.resumeAt(8).get(T));

		memory.put(T, 10);
		memory.put(T, 10);
		memory.put(T, 10);
		assertDiff(memory.resumeAt(10).get(T), +10, -12);

		assertDiff(memory.resumeAt(12).get(T));

		assertDiff(memory.resumeAt(14).get(T), +14);
	}

	private TimelyMemory<Integer> createMemory() {
		return new TimelyMemory<Integer>(true);
	}

	private void assertDiff(final Iterable<Signed<Integer>> elements, final int... timestamps) {
		assert StreamSupport.stream(elements.spliterator(), false).count() == timestamps.length;
		int i = 0;
		for (final Signed<Integer> element : elements) {
			Integer diffTimestamp = element.getPayload();
			if (element.getDirection() == Direction.DELETE) {
				diffTimestamp *= -1;
			}
			assert diffTimestamp == timestamps[i];
			i++;
		}
	}

}
