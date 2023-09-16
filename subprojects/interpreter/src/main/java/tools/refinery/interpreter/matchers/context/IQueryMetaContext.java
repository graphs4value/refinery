/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Provides metamodel information (relationship of input keys) to query evaluator backends at runtime and at query planning time.
 *
 * @noimplement Implementors should extend {@link AbstractQueryMetaContext} instead of directly implementing this interface.
 * @author Bergmann Gabor
 */
public interface IQueryMetaContext {

	/**
	 * Returns true iff instance tuples of the given key can be enumerated.
	 * <p> If false, the runtime can only test tuple membership in the extensional relation identified by the key, but not enumerate member tuples in general.
	 * <p> Equivalent to {@link IInputKey#isEnumerable()}.
	 */
	boolean isEnumerable(IInputKey key);

	/**
	 * Returns true iff the set of instance tuples of the given key is immutable.
	 * <p> If false, the runtime provides notifications upon change.
	 */
	boolean isStateless(IInputKey key);

    /**
     * Returns a set of implications (weakened alternatives),
     *  with a suggestion for the query planner that satisfying them first may help in satisfying the implying key.
     * <p> Note that for the obvious reasons, enumerable keys can only be implied by enumerable keys.
     * <p> Must follow directly or transitively from implications of {@link #getImplications(IInputKey)}.
     * @since 1.6
     */
    Collection<InputKeyImplication> getWeakenedAlternatives(IInputKey implyingKey);

	/**
	 * Returns known direct implications, e.g. edge supertypes, edge opposites, node type constraints, etc.
     * <p> Note that for the obvious reasons, enumerable keys can only be implied by enumerable keys.
	 */
	Collection<InputKeyImplication> getImplications(IInputKey implyingKey);

    /**
     * Returns known "double dispatch" implications, where the given implying key implies other input keys under certain additional conditions (themselves input keys).
     * For example, a "type x, unscoped" input key may imply the "type x, in scope" input key under the condition of the input key "x is in scope"
     *
     * <p> Note that for the obvious reasons, enumerable keys can only be implied by enumerable keys (either as the implying key or as the additional condition).
     * <p> Note that symmetry is not required, i.e. the additional conditions do not have to list the same conditional implication.
     * @return multi-map, where the keys are additional conditions and the values are input key implications jointly implied by the condition and the given implying key.
     * @since 2.0
     */
    Map<InputKeyImplication, Set<InputKeyImplication>> getConditionalImplications(IInputKey implyingKey);

	/**
	 * Returns functional dependencies of the input key expressed in terms of column indices.
	 *
	 * <p> Each entry of the map is a functional dependency rule, where the entry key specifies source columns and the entry value specifies target columns.
	 */
	Map<Set<Integer>, Set<Integer>> getFunctionalDependencies(IInputKey key);

	/**
	 * For query normalizing, this is the order suggested for trying to eliminate input keys.
	 * @since 1.6
	 */
	Comparator<IInputKey> getSuggestedEliminationOrdering();

	/**
	 * Tells whether the given {@link IInputKey} is an edge and may lead out of scope.
	 *
	 * @since 1.6
	 */
	boolean canLeadOutOfScope(IInputKey key);

	/**
	 * Returns true if the given {@link IInputKey} represents a poset type.
	 * @since 1.6
	 */
	boolean isPosetKey(IInputKey key);

	/**
	 * Returns an {@link IPosetComparator} for the given set of {@link IInputKey}s.
	 *
	 * @param keys an iterable collection of input keys
	 * @return the poset comparator
	 * @since 1.6
	 */
	IPosetComparator getPosetComparator(Iterable<IInputKey> keys);

}
