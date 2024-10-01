/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.tests;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import tools.refinery.interpreter.matchers.tuple.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for tuples to ensure equivalence between implementations
 * @author Gabor Bergmann
 *
 */
@RunWith(Parameterized.class)
public class TupleTest {

	public static final int SPECIALIZED_ARITY_LIMIT = 4;
	public static final Tuple ANCESTOR = Tuples.staticArityFlatTupleOf(-3, -2, -1);
	public static final Tuple MASKABLE_TUPLE = Tuples.flatTupleOf(-100, -200, -300);

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
	public void testFlatTuples() {
		boolean highArity = arity > SPECIALIZED_ARITY_LIMIT;
		Tuple tuple = Tuples.flatTupleOf(values);

		assertEquals("size", arity, tuple.getSize());
		assertTrue("baseClass", tuple instanceof BaseFlatTuple);
		assertEquals("specialized iff low arity", highArity, (tuple instanceof FlatTuple));
		for (int i=0; i<arity; ++i) {
			assertEquals("get" + i, i, tuple.get(i));
		}
		assertArrayEquals("elements[]", values, tuple.getElements());

		Tuple flatTupleReference = Tuples.wideFlatTupleOf(values);
		assertTrue("equality(ft)",  flatTupleReference.equals(tuple));
		assertTrue("equality(spec)", tuple.equals(flatTupleReference));
		assertTrue("equality(other)", tuple.equals(Tuples.flatTupleOf(values)));
		assertEquals("hashCode", flatTupleReference.hashCode(), tuple.hashCode());
	}

	@Test
	public void testLeftInheritanceTuples() {
		for (int localArity = 0; localArity <= SPECIALIZED_ARITY_LIMIT + 1; ++localArity) {
			int totalArity = ANCESTOR.getSize() + localArity;
			Object[] allValues   = new Object[totalArity];
			Object[] localValues = new Object[localArity];
			int k;
			for (k=0; k<ANCESTOR.getSize(); ++k) allValues[k] = ANCESTOR.get(k);
			for (int i=0; i<localArity; ++i) localValues[i] = allValues[k++] = i;

			boolean highArity = localArity > SPECIALIZED_ARITY_LIMIT;
			Tuple liTuple = Tuples.leftInheritanceTupleOf(ANCESTOR, localValues);

			assertEquals("size", totalArity, liTuple.getSize());
			assertEquals("baseClass", localArity != 0, liTuple instanceof BaseLeftInheritanceTuple);
			assertEquals("specialized iff low arity", highArity, (liTuple instanceof LeftInheritanceTuple));
			for (int i=0; i<totalArity; ++i) {
				assertEquals("get" + i, i - ANCESTOR.getSize(), liTuple.get(i));
			}
			assertArrayEquals("elements[]", allValues, liTuple.getElements());

			Tuple liTupleReference = Tuples.wideLeftInheritanceTupleOf(ANCESTOR, localValues);
			assertTrue("equality(lit)",  liTupleReference.equals(liTuple));
			assertTrue("equality(spec)", liTuple.equals(liTupleReference));
			assertTrue("equality(other)", liTuple.equals(Tuples.leftInheritanceTupleOf(ANCESTOR, localValues)));
			assertEquals("hashCode", liTupleReference.hashCode(), liTuple.hashCode());

			Tuple flatTupleReference = Tuples.flatTupleOf(allValues);
			assertTrue("equality(lit)",  flatTupleReference.equals(liTuple));
			assertTrue("equality(spec)", liTuple.equals(flatTupleReference));
			assertEquals("hashCode", flatTupleReference.hashCode(), liTuple.hashCode());
		}
	}

	@Test
	public void testMasks() {
		ArrayList<Integer> selectedIndices = new ArrayList<>();
		recursiveBuildIndices(selectedIndices);
	}

	private void recursiveBuildIndices(ArrayList<Integer> selectedIndices) {
		checkIndicesArray(selectedIndices);

		// grow
		int startingSize = selectedIndices.size();
		if (startingSize < MASKABLE_TUPLE.getSize()) {
			for (int nextIndex=0; nextIndex < MASKABLE_TUPLE.getSize(); ++nextIndex) {
				selectedIndices.add(nextIndex);

				recursiveBuildIndices(selectedIndices);

				// undo
				selectedIndices.remove(startingSize);
			}

		}
	}

	private void checkIndicesArray(List<Integer> selectedIndices) {
		TupleMask mask = TupleMask.fromSelectedIndices(MASKABLE_TUPLE.getSize(), selectedIndices);

		Tuple maskedTuple = mask.transform(MASKABLE_TUPLE);
		assertEquals("maskedResultWidth", selectedIndices.size(), maskedTuple.getSize());
		for (int k=0; k < selectedIndices.size(); ++k) {
			assertEquals("maskedResult["+k, MASKABLE_TUPLE.get(selectedIndices.get(k)), maskedTuple.get(k));
		}

		if (!selectedIndices.isEmpty()) {
			boolean isIdentity = selectedIndices.size() == MASKABLE_TUPLE.getSize();
			int j = 0;
			while (isIdentity && j < selectedIndices.size()) {
				if (selectedIndices.get(j) != j) isIdentity = false;
				++j;
			}
			assertEquals("identity", isIdentity, mask instanceof TupleMaskIdentity);
		}

		Tuple combinedTuple = mask.combine(ANCESTOR, MASKABLE_TUPLE, true, true);
		assertEquals("combinedResultWidth", selectedIndices.size() + ANCESTOR.getSize(), combinedTuple.getSize());
		for (int k=0; k < combinedTuple.getSize(); ++k) {
			assertEquals("combinedResult["+k,
					(k<ANCESTOR.getSize()) ?
							ANCESTOR.get(k) :
							MASKABLE_TUPLE.get(selectedIndices.get(k - ANCESTOR.getSize())),
					combinedTuple.get(k));
		}
		assertEquals("nullary", selectedIndices.isEmpty(), combinedTuple == ANCESTOR);

	}

	@Test
	public void simpleMaskTest() {
		Assume.assumeTrue(arity > 2);
		Tuple tuple = Tuples.flatTupleOf(values);
		Tuple expectedResult = Tuples.staticArityFlatTupleOf(0, 0);
		TupleMask mask = TupleMask.fromSelectedIndices(arity, new int[] {0, 0});
		Tuple actualResult = mask.transform(tuple);
		assertEquals(expectedResult, actualResult);
	}
}
