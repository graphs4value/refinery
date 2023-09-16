/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc.alg.util;

import tools.refinery.interpreter.matchers.util.CollectionsFactory;

import java.util.Set;

/**
 * @author Tamas Szabo
 *
 */
public class CollectionHelper {

    private CollectionHelper() {/*Utility class constructor*/}

    /**
     * Returns the intersection of two sets. It calls {@link Set#retainAll(java.util.Collection)} but returns a new set
     * containing the elements of the intersection.
     *
     * @param set1
     *            the first set (can be null, interpreted as empty)
     * @param set2
     *            the second set (can be null, interpreted as empty)
     * @return the intersection of the sets
     * @since 1.7
     */
    public static <V> Set<V> intersection(Set<V> set1, Set<V> set2) {
        if (set1 == null || set2 == null)
            return CollectionsFactory.createSet();

        Set<V> intersection = CollectionsFactory.createSet(set1);
        intersection.retainAll(set2);
        return intersection;
    }


    /**
     * Returns the difference of two sets (S1\S2). It calls {@link Set#removeAll(java.util.Collection)} but returns a
     * new set containing the elements of the difference.
     *
     * @param set1
     *            the first set (can be null, interpreted as empty)
     * @param set2
     *            the second set (can be null, interpreted as empty)
     * @return the difference of the sets
     * @since 1.7
     */
    public static <V> Set<V> difference(Set<V> set1, Set<V> set2) {
        if (set1 == null)
            return CollectionsFactory.createSet();

        Set<V> difference = CollectionsFactory.createSet(set1);
        if (set2 != null) difference.removeAll(set2);
        return difference;
    }

}
