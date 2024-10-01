/*******************************************************************************
 * Copyright (c) 2010-2013, Adam Dudas, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.tests;

import org.junit.Test;
import tools.refinery.interpreter.matchers.planning.helpers.FunctionalDependencyHelper;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static tools.refinery.interpreter.matchers.planning.helpers.FunctionalDependencyHelper.closureOf;
import static tools.refinery.interpreter.matchers.planning.helpers.FunctionalDependencyHelper.projectDependencies;

/**
 * Tests for {@link FunctionalDependencyHelper}.
 *
 * @author Adam Dudas
 *
 */
public class FunctionalDependencyHelperTest {

	private static final Set<Object> emptySet = Set.<Object> of();
	private static final Map<Set<Object>, Set<Object>> emptyMap = Map.<Set<Object>, Set<Object>> of();
	private static final Object a = new Object();
	private static final Object b = new Object();
	private static final Object c = new Object();
	private static final Object d = new Object();
	private static final Object e = new Object();
	private static final Map<Set<Object>, Set<Object>> testDependencies = Map.<Set<Object>, Set<Object>> of(
			Set.of(a, b), Set.of(c), // AB -> C
			Set.of(a), Set.of(d), // A -> D
			Set.of(d), Set.of(e), // D -> E
			Set.of(a, c), Set.of(b)); // AC -> B

	@Test
	public void testClosureOfEmptyAttributeSetEmptyDependencySet() {
		assertEquals(emptySet, closureOf(emptySet, emptyMap));
	}

	@Test
	public void testClosureOfEmptyAttributeSet() {
		assertEquals(emptySet, closureOf(emptySet, testDependencies));
	}

	@Test
	public void testClosureOfEmptyDependencySet() {
		Set<Object> X = Set.of(a, b, c, d);
		assertEquals(X, closureOf(X, emptyMap));
	}

	@Test
	public void testClosureOf() {
		assertEquals(Set.of(a, d, e), closureOf(Set.of(a), testDependencies));
		assertEquals(Set.of(a, b, c, d, e), closureOf(Set.of(a, b), testDependencies));
		assertEquals(Set.of(a, b, c, d, e), closureOf(Set.of(a, c), testDependencies));
		assertEquals(Set.of(b), closureOf(Set.of(b), testDependencies));
		assertEquals(Set.of(d, e), closureOf(Set.of(d), testDependencies));
	}

	@Test
	public void testProject() {
		assertEquals(Collections.emptyMap(), projectDependencies(testDependencies, Set.of(a)));
		assertEquals(Map.of(Collections.singleton(a), Set.of(a, e)),
				projectDependencies(testDependencies, Set.of(a, c, e)));
	}

}
