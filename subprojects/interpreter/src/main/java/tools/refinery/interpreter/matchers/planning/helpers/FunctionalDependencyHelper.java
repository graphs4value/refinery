/*******************************************************************************
 * Copyright (c) 2010-2013, Adam Dudas, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning.helpers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tools.refinery.interpreter.matchers.util.Sets;

/**
 * Helper utility class for functional dependency analysis.
 *
 * Throughout this class attribute sets are represented as generic sets and functional dependencies as maps from
 * attribute set (generic sets) to attribute set (generic sets)
 *
 * @author Adam Dudas
 *
 */
public class FunctionalDependencyHelper {

    private FunctionalDependencyHelper() {
        // Hiding constructor for utility class
    }

    /**
     * Get the closure of the specified attribute set relative to the specified functional dependencies.
     *
     * @param attributes
     *            The attributes to get the closure of.
     * @param dependencies
     *            The functional dependencies of which the closure operation is relative to.
     * @return The closure of the specified attribute set relative to the specified functional dependencies.
     */
    public static <A> Set<A> closureOf(Collection<A> attributes, Map<Set<A>, Set<A>> dependencies) {
        Set<A> closureSet = new HashSet<A>();

        for (Set<A> closureSet1 = new HashSet<A>(attributes); closureSet.addAll(closureSet1);) {
            closureSet1 = new HashSet<A>();
            for (Entry<Set<A>, Set<A>> dependency : dependencies.entrySet()) {
                if (closureSet.containsAll(dependency.getKey()))
                    closureSet1.addAll(dependency.getValue());
            }
        }

        return closureSet;
    }

    /**
     * @return true if the dependency from the left set to the right set is trivial
     * @since 1.5
     */
    public static <A> boolean isTrivial(Set<A> left, Set<A> right) {
        return left.containsAll(right);
    }

    /***
     * Returns the dependency set over attributes in {@link targetAttributes} that are implied by a given source dependency set.
     * <p> Note: exponential in the size of the target attribute set.
     * <p> Note: minimality of the returned dependency set is currently not guaranteed.
     * @param originalDependencies all dependencies that are known to hold on a wider set of attributes
     * @param targetAttributes the set of attributes we are interested in
     * @since 1.5
     */
    public static <A> Map<Set<A>, Set<A>> projectDependencies(Map<Set<A>, Set<A>> originalDependencies, Set<A> targetAttributes) {
        // only those attributes are considered as left-hand-side candidates that occur at least once in dependencies
        Set<A> leftCandidates = new HashSet<A>();
        for (Entry<Set<A>, Set<A>> dependency : originalDependencies.entrySet()) {
            if (!isTrivial(dependency.getKey(), dependency.getValue())) // only if non-trivial
                leftCandidates.addAll(Sets.intersection(dependency.getKey(), targetAttributes));
        }

        // Compute an initial list of nontrivial projected dependencies - it does not have to be minimal yet
        Map<Set<A>, Set<A>> initialDependencies = new HashMap<Set<A>, Set<A>>();
        for (Set<A> leftSet : Sets.powerSet(leftCandidates)) {
            Set<A> rightSet = Sets.intersection(closureOf(leftSet, originalDependencies), targetAttributes);
            if (!isTrivial(leftSet, rightSet)) {
                initialDependencies.put(leftSet, rightSet);
            }
        }
        // Don't forget to include constants!
        Set<A> constants = Sets.intersection(closureOf(Collections.<A>emptySet(), originalDependencies), targetAttributes);
        if (! constants.isEmpty()) {
            initialDependencies.put(Collections.<A>emptySet(), constants);
        }

        // Omit those dependencies where the LHS has superfluous attributes
        Map<Set<A>, Set<A>> solidDependencies = new HashMap<Set<A>, Set<A>>();
        for (Entry<Set<A>, Set<A>> dependency : initialDependencies.entrySet()) {
            Set<A> leftSet = dependency.getKey();
            Set<A> rightSet = dependency.getValue();
            boolean solid = true;
            for (A skipped : leftSet) { // what if we skip one attribute from the left set?
                Set<A> singleton = Collections.singleton(skipped);
                Set<A> candidate = Sets.difference(leftSet, singleton);
                Set<A> rightCandidate = initialDependencies.get(candidate);
                if (rightCandidate != null) {
                    if (Sets.union(rightCandidate, singleton).containsAll(rightSet)) {
                        solid = false;
                        break;
                    }
                }
            }
            if (solid) {
                solidDependencies.put(leftSet, rightSet);
            }
        }

        // TODO perform proper minimization,
        // see e.g. page 45 in http://www.cs.ubc.ca/~hkhosrav/db/slides/03.design%20theory.pdf

        return Collections.unmodifiableMap(solidDependencies);
    }

    /**
     * Adds a given dependency to a mutable accumulator.
     * @since 1.5
     */
    public static <A> void includeDependency(Map<Set<A>, Set<A>> accumulator, Set<A> left, Set<A> right) {
        Set<A> accumulatorRights = accumulator.computeIfAbsent(left, l -> new HashSet<>());
        accumulatorRights.addAll(right);
    }

    /**
     * Adds all given dependencies to a mutable accumulator.
     * @since 1.5
     */
    public static <A> void includeDependencies(Map<Set<A>, Set<A>> accumulator, Map<Set<A>, Set<A>> additionalDependencies) {
        for (Entry<Set<A>, Set<A>> entry : additionalDependencies.entrySet()) {
            includeDependency(accumulator, entry.getKey(), entry.getValue());
        }
    }
}
