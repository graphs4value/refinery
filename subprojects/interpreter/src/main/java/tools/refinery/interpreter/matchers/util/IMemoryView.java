/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A read-only view on a memory containing a positive or negative number of equal() copies for some values.
 * During iterations, each distinct value is iterated only once.
 *
 * <p> See {@link IMemory}.
 *
 * <p> Implementors must provide semantic (not identity-based) hashCode() and equals() using the static helpers {@link #hashCode(IMemoryView)} and {@link #equals(IMemoryView, Object)} here.
 *
 * @author Gabor Bergmann
 *
 * @since 2.0
 */
public interface IMemoryView<T> extends Iterable<T> {

    /**
     * Returns the number of occurrences of the given value.
     *
     * @return the number of occurrences
     */
    int getCount(T value);

    /**
     * Returns the number of occurrences of the given value (which may be of any type).
     *
     * @return the number of occurrences
     */
    int getCountUnsafe(Object value);

    /**
     * @return true if the given value is contained with a nonzero multiplicity
     */
    boolean containsNonZero(T value);

    /**
     * @return true if the given value (which may be of any type) is contained with a nonzero multiplicity
     */
    boolean containsNonZeroUnsafe(Object value);

    /**
     * @return the number of distinct values
     */
    int size();

    /**
     *
     * @return iff contains at least one value with non-zero occurrences
     */
    boolean isEmpty();

    /**
     * The set of distinct values
     */
    Set<T> distinctValues();


    /**
     * Where supported, returns the stored element that is equal to the given value, or null if none.
     * Useful for canonicalization in case of non-identity equals().
     *
     * <p> For collections that do not support canonicalization, simply returns the argument if contained, null if none.
     *
     * @return a value equal to the argument if such a value is stored, or null if none
     */
    default T theContainedVersionOf(T value) {
        if (containsNonZero(value)) return value; else return null;
    }

    /**
     * Where supported, returns the stored element that is equal to the given value (of any type),
     * or null if none.
     * Useful for canonicalization in case of non-identity equals().
     *
     * <p> For collections that do not support canonicalization, simply returns the argument if contained, null if none.
     *
     * @return a value equal to the argument if such a value is stored, or null if none
     */
    @SuppressWarnings("unchecked")
    default T theContainedVersionOfUnsafe(Object value) {
        if (containsNonZeroUnsafe(value)) return (T) value; else return null;
    }


    /**
     * @return an unmodifiable view of contained values with their multiplicities
     */
    default Iterable<Map.Entry<T, Integer>> entriesWithMultiplicities() {
        return () -> {
            Iterator<T> wrapped = distinctValues().iterator();
            return new Iterator<Map.Entry<T, Integer>> () {
                @Override
                public boolean hasNext() {
                    return wrapped.hasNext();
                }

                @Override
                public Map.Entry<T, Integer> next() {
                    T key = wrapped.next();
                    int count = getCount(key);
                    return new Map.Entry<T, Integer>(){
                        @Override
                        public T getKey() {
                            return key;
                        }

                        @Override
                        public Integer getValue() {
                            return count;
                        }

                        @Override
                        public Integer setValue(Integer value) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public String toString() {
                            return String.format("%d of %s", count, key);
                        }

                    };
                }

            };
        };
    }

    /**
     * Process contained values with their multiplicities
     */
    default void forEachEntryWithMultiplicities(BiConsumer<T, Integer> entryConsumer) {
        for (T value : distinctValues()) {
            entryConsumer.accept(value, getCount(value));
        }
    }


    /**
     * For compatibility with legacy code relying on element-to-integer maps.
     * @return an unmodifiable view of contained values with their multiplicities
     */
    public default Map<T, Integer> asMap() {
        return new MemoryViewBackedMapView<>(this);
    }

    /**
     * For compatibility with legacy code relying on element-to-integer maps.
     * @return an unmodifiable view of contained values with their multiplicities
     */
    public static <T> IMemoryView<T> fromMap(Map<T, Integer> wrapped) {
        return new MapBackedMemoryView<>(wrapped);
    }

    /**
     * @return a stream of values, iterable once
     * @since 2.1
     */
    public default Stream<T> asStream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Provides semantic equality comparison.
     */
    public static <T> boolean equals(IMemoryView<T> self, Object obj) {
        if (obj instanceof IMemoryView<?>) {
            IMemoryView<?> other = (IMemoryView<?>) obj;
            if (other.size() != self.size()) return false;
            for (Entry<?, Integer> entry : other.entriesWithMultiplicities()) {
                if ( !entry.getValue().equals(self.getCountUnsafe(entry.getKey())))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Provides semantic hashCode() comparison.
     */
    public static <T> int hashCode(IMemoryView<T> memory) {
        int hashCode = 0;
        for (T value : memory.distinctValues()) {
            hashCode += value.hashCode() ^ Integer.hashCode(memory.getCount(value));
        }
        return hashCode;
    }
}
