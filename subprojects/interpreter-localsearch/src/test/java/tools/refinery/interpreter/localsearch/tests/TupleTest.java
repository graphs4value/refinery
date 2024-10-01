/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, Istvan Rath and Daniel Varro
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * This file has been split off from {@link tools.refinery.interpreter.tests.TupleTest}
 * to comply with the simplified project layout of Refinery.
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * Tests for tuples to ensure equivalence between implementations
 * @author Gabor Bergmann
 *
 */
@RunWith(Parameterized.class)
public class TupleTest {

	@Parameters()
	public static Collection<Object[][]> data() {
		return Arrays.asList(
				new Object[][] {{}},
				new Object[][] {{0}},
				new Object[][] {{0, 1}},
				new Object[][] {{0, 1, 2}},
				new Object[][] {{0, 1, 2, 3}},
				new Object[][] {{0, 1, 2, 3, 4}}
		);
	}

	private final Object[] values;
	private final int arity;



	public TupleTest(Object[] values) {
		super();
		this.values = values;
		this.arity = values.length;
	}

	@Test
	public void testVolatileTuples() {
		MatchingFrame frame = new MatchingFrame(arity);
		for (int i=0; i<arity; ++i) {
			frame.set(i, values[i]);
		}
		ITuple tuple = Tuples.flatTupleOf(values);

		assertTrue("equality", Objects.equals(tuple, frame));
		assertEquals("hashCode", tuple.hashCode(), frame.hashCode());

		if (arity > 0) {
			frame.setValue(0, "x");
			assertFalse("equality", Objects.equals(tuple, frame));
			assertNotEquals("hashCode", tuple.hashCode(), frame.hashCode());
		}
	}

	@Test
	public void testToImmutable() {
		MatchingFrame frame = new MatchingFrame(arity);
		for (int i=0; i<arity; ++i) {
			frame.set(i, values[i]);
		}
		Tuple tuple = Tuples.flatTupleOf(values);

		ITuple tupleFromTuple = tuple.toImmutable();
		ITuple tupleFromFrame = frame.toImmutable();
		assertTrue("equality tuple", Objects.equals(tuple, tupleFromTuple));
		assertTrue("equality tuple", Objects.equals(tupleFromTuple, tuple));
		assertTrue("equality frame", Objects.equals(frame, tupleFromFrame));
		assertTrue("equality frame", Objects.equals(tupleFromFrame, frame));

		if (arity > 0) {
			frame.setValue(0, "x");
			assertFalse("equality frame", Objects.equals(frame, tupleFromFrame));
			assertFalse("equality frame", Objects.equals(tupleFromFrame, frame));
		}
	}
}
