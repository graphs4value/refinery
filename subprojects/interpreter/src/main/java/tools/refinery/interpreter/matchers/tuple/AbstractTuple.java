/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Common implementation methods for immutable and volatile tuples. The class should not be used directly in client
 * code, except for the definition of new tuple implementations.
 *
 * @author Zoltan Ujhelyi
 * @since 1.7
 */
public abstract class AbstractTuple implements ITuple {

    /**
     * As the tuple is supposed to be immutable, do not modify the returned array.
     *
     * @return the array containing all elements of this Tuple
     */
    @Override
    public Object[] getElements() {
        Object[] allElements = new Object[getSize()];
        for (int i = 0; i < allElements.length; ++i)
            allElements[i] = get(i);
        return allElements;
    }

    /**
     * @return the set containing all distinct elements of this Tuple, cast as type T
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> getDistinctElements() {
        Set<T> result = new HashSet<T>();
        Object[] elements = getElements();
        for (Object object : elements) {
            result.add((T) object);
        }
        return result;
    }

    /**
     * Calculates an inverted index of the elements of this pattern. For each element, the index of the (last)
     * occurrence is calculated.
     *
     * @return the inverted index mapping each element of this pattern to its index in the array
     */
    @Override
    public Map<Object, Integer> invertIndex() {
        Map<Object, Integer> result = new HashMap<Object, Integer>();
        for (int i = 0; i < getSize(); i++)
            result.put(get(i), i);
        return result;
    }

    /**
     * Calculates an inverted index of the elements of this pattern. For each element, the index of all of its
     * occurrences is calculated.
     *
     * @return the inverted index mapping each element of this pattern to its index in the array
     */
    @Override
    public Map<Object, List<Integer>> invertIndexWithMupliplicity() {
        Map<Object, List<Integer>> result = new HashMap<Object, List<Integer>>();
        for (int i = 0; i < getSize(); i++) {
            Object value = get(i);
            List<Integer> indices = result.computeIfAbsent(value, v -> new ArrayList<>());
            indices.add(i);
        }
        return result;
    }

    /**
     * @since 1.7
     */
    protected IndexOutOfBoundsException raiseIndexingError(int index) {
        return new IndexOutOfBoundsException(
                String.format("No value at position %d for %s instance %s", index, getClass().getSimpleName(), this));
    }

    /**
     * Compares the elements stored in this tuple to another tuple
     */
    protected boolean internalEquals(ITuple other) {
        if (getSize() != other.getSize())
            return false;
        boolean result = true;
        for (int i = 0; result && i < getSize(); ++i) {
            Object ours = get(i);
            Object theirs = other.get(i);
            result = result && Objects.equals(ours, theirs);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("T(");
        for (Object o : getElements()) {
            s.append(o == null ? "null" : o.toString());
            s.append(';');
        }
        s.append(')');
        return s.toString();
    }

    /**
     * @since 1.7
     */
    protected int doCalcHash() {
        final int PRIME = 31;
        int hash = 1;
        for (int i = 0; i < getSize(); i++) {
            hash = PRIME * hash;
            Object element = get(i);
            if (element != null)
                hash += element.hashCode();
        }
        return hash;
    }

}
