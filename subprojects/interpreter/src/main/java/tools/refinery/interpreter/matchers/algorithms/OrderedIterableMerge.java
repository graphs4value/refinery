/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.algorithms;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Gabor Bergmann
 * @since 2.1
 *
 */
public class OrderedIterableMerge {

    private OrderedIterableMerge() {
        // Hidden utility class constructor
    }

    /**
     * Lazily merges two iterables, each ordered according to a given comparator.
     * Retains order in the result, and also eliminates any duplicates that appear in both arguments.
     */
    public static <T> Iterable<T> mergeUniques(Iterable<T> first, Iterable<T> second, Comparator<T> comparator) {
        return () -> new Iterator<T>() {
            Iterator<T> firstIterator = first.iterator();
            Iterator<T> secondIterator = second.iterator();
            T firstItem;
            T secondItem;

            {
                fetchFirst();
                fetchSecond();
            }

            private T fetchFirst() {
                T previous = firstItem;
                if (firstIterator.hasNext())
                    firstItem = firstIterator.next();
                else
                    firstItem = null;
                return previous;
            }
            private T fetchSecond() {
               T previous = secondItem;
               if (secondIterator.hasNext())
                    secondItem = secondIterator.next();
                else
                    secondItem = null;
               return previous;
            }

            @Override
            public boolean hasNext() {
                return firstItem != null || secondItem != null;
            }
            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                if (firstItem != null && secondItem != null) {
                    if (secondItem == firstItem) { // duplicates
                        fetchFirst();
                        return fetchSecond();
                    } else if (comparator.compare(firstItem, secondItem) < 0) {
                        return fetchFirst();
                    } else {
                        return fetchSecond();
                    }
                } else if (firstItem != null) {
                    return fetchFirst();
                } else { // secondItem must be non-null
                    return fetchSecond();
                }
            }
        };
    }
}
