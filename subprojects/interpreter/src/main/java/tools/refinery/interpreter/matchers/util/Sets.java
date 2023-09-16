/*******************************************************************************
 * Copyright (c) 2010-2019, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Gabor Bergmann - initial API and implementation
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class was motivated by the similar Sets class from Guava to provide simple set manipulation
 * functionality. However, as starting with version 2.3 the runtime of VIATRA Query should not depend on Guava,
 * not even internally, the relevant subset of Sets methods will be reimplemented here.
 *
 * <p> The current approach is to delegate to Eclipse Collections wherever possible.
 * Such glue methods are useful so that downstream clients can avoid directly depending on Eclipse Collections.
 *
 * <p> Without an equivalent from Eclipse Collections, {@link #cartesianProduct(List)} is implemented here from scratch.
 *
 * @author Gabor Bergmann
 * @since 2.3
 */
public final class Sets {

    /**
     * @since 2.4
     */
    public static <A> Set<A> newSet(Iterable<A> elements) {
        return org.eclipse.collections.impl.factory.Sets.mutable.ofAll(elements);
    }

    public static <A> Set<A> intersection(Set<A> left, Set<A> right) {
        return org.eclipse.collections.impl.factory.Sets.intersect(left, right);
    }

    public static <A> Set<A> difference(Set<A> left, Set<A> right) {
        return org.eclipse.collections.impl.factory.Sets.difference(left, right);
    }

    public static <A> Set<A> union(Set<A> left, Set<A> right) {
        return org.eclipse.collections.impl.factory.Sets.union(left, right);
    }

    public static <A> Set<? extends Set<A>> powerSet(Set<A> set) {
        return org.eclipse.collections.impl.factory.Sets.powerSet(set);
    }

    public static <A> Set<List<A>> cartesianProduct(List<? extends Set<? extends A>> setsList) {

        class Suffix { // simple immutable linked list
            private A head;
            private Suffix next;

            public Suffix(A head, Suffix next) {
                super();
                this.head = head;
                this.next = next;
            }

            public List<A> toList() {
                ArrayList<A> result = new ArrayList<>();
                for (Suffix cursor = this; cursor!=null; cursor = cursor.next)
                    result.add(cursor.head);
                return result;
            }
        }

        // build result lists from end to start, in the form of suffixes
        Stream<Suffix> suffixes = Stream.of((Suffix) null /* empty suffix*/);
        for (int i = setsList.size()-1; i>=0; --i) { // iterate sets in reverse order
            Set<? extends A> set = setsList.get(i);
            suffixes = suffixes.flatMap(suffix -> set.stream().map(newElement -> new Suffix(newElement, suffix)));
        }


        return suffixes.map(Suffix::toList).collect(Collectors.toSet());
    }


}
