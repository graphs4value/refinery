/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents both mutable and immutable tuples
 *
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public interface ITuple {

    /**
     * @pre: 0 <= index < getSize()
     *
     * @return the element at the specified index
     */
    Object get(int index);

    /**
     * As the tuple is supposed to be immutable, do not modify the returned array.
     * @return the array containing all elements of this Tuple
     */
    Object[] getElements();

    /**
     * @return the set containing all distinct elements of this Tuple, cast as type T
     */
    <T> Set<T> getDistinctElements();

    /**
     * @return number of elements
     */
    int getSize();

    /**
     * Calculates an inverted index of the elements of this pattern. For each element, the index of the (last)
     * occurrence is calculated.
     *
     * @return the inverted index mapping each element of this pattern to its index in the array
     */
    Map<Object, Integer> invertIndex();

    /**
     * Calculates an inverted index of the elements of this pattern. For each element, the index of all of its
     * occurrences is calculated.
     *
     * @return the inverted index mapping each element of this pattern to its index in the array
     */
    Map<Object, List<Integer>> invertIndexWithMupliplicity();

    Tuple toImmutable();
}
