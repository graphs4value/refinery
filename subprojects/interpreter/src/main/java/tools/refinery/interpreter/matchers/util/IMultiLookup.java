/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.util.CollectionsFactory.MemoryType;

/**
 * A multi-map that associates sets / multisets / delta sets of values to each key.
 *
 * <p> Implementors must provide semantic (not identity-based) hashCode() and equals() using the static helpers {@link #hashCode(IMultiLookup)} and {@link #equals(IMultiLookup, Object)} here.
 *
 * @author Gabor Bergmann
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMultiLookup<Key, Value> {

    /**
     * Returns true if this collection is empty, false otherwise.
     * @since 2.2
     */
    boolean isEmpty();


    /**
     * Returns true if there are any values associated with the given key.
     * @param key a key for which associated values are sought
     * @since 2.3
     */
    boolean lookupExists(Key key);

    /**
     * Returns a (read-only) bucket of values associated with the given key.
     * Clients must not modify the returned bucket.
     * @param key a key for which associated values are sought
     * @return null if key not found, a bucket of values otherwise
     */
    IMemoryView<Value> lookup(Key key);

    /**
     * Returns a (read-only) bucket of values associated with the given key.
     * Clients must not modify the returned bucket.
     * @param key a key for which associated values are sought
     * @return a bucket of values, never null
     */
    default IMemoryView<Value> lookupOrEmpty(Key key) {
        IMemoryView<Value> bucket = lookup(key);
        return bucket == null ? EmptyMemory.instance() : bucket;
    }

    /**
     * Returns a (read-only) bucket of values associated with the given key, while simultaneously removing them.
     * Clients must not modify the returned bucket.
     * @param key a key for which associated values are sought
     * @return a bucket of values, never null
     * @since 2.3
     */
    IMemoryView<Value> lookupAndRemoveAll(Key key);

    /**
     * Returns a (read-only) bucket of values associated with the given key, which can be of any type.
     * Clients must not modify the returned bucket.
     * @param key a key for which associated values are sought (may or may not be of Key type)
     * @return null if key not found, a bucket of values otherwise
     */
    IMemoryView<Value> lookupUnsafe(Object key);

    /**
     * Returns a (read-only) bucket of values associated with the given key.
     * Clients must not modify the returned bucket.
     * @param key a key for which associated values are sought (may or may not be of Key type)
     * @return a bucket of values, never null
     */
    default IMemoryView<Value> lookupUnsafeOrEmpty(Object key) {
        IMemoryView<Value> bucket = lookupUnsafe(key);
        return bucket == null ? EmptyMemory.instance() : bucket;
    }



    /**
     * @return the set of distinct keys that have values associated.
     */
    Iterable<Key> distinctKeys();

    /**
     * @return the set of distinct keys that have values associated.
     * @since 2.3
     */
    Stream<Key> distinctKeysStream();

    /**
     * @return the number of distinct keys that have values associated.
     */
    int countKeys();

    /**
     * Iterates once over each distinct value.
     */
    Iterable<Value> distinctValues();

    /**
     * Iterates once over each distinct value.
     * @since 2.3
     */
    Stream<Value> distinctValuesStream();



    /**
     * How significant was the change?     *
     * @author Gabor Bergmann
     */
    public enum ChangeGranularity {
        /**
         * First key-value pair with given key inserted, or last pair with given key deleted.
         * (In case of delta maps, also if last negative key-value pair with given key neutralized.)
         */
        KEY,
        /**
         * First occurrence of given key-value pair inserted, or last occurrence of the pair deleted, while key still has values associated.
         * (In case of delta maps, also if last negative occurrence of key-value pair neutralized.)
         */
        VALUE,
        /**
         * Duplicate key-value pair inserted or deleted.
         */
        DUPLICATE
    }

    /**
     * Adds key-value pair to the lookup structure, or fails if not possible.
     * <p> If the addition would cause duplicates but the bucket type does not allow it ({@link MemoryType#SETS}),
     *   the operation throws an {@link IllegalStateException}.
     * @return the granularity of the change
     * @throws IllegalStateException if addition would cause duplication that is not permitted
     */
    public ChangeGranularity addPair(Key key, Value value);
    /**
     * Adds key-value pair to the lookup structure.
     * <p> If the addition would cause duplicates but the bucket type does not allow it ({@link MemoryType#SETS}),
     *   the operation is silently ignored and {@link ChangeGranularity#DUPLICATE} is returned.
     * @return the granularity of the change, or {@link ChangeGranularity#DUPLICATE} if addition would result in a duplicate and therefore ignored
     * @since 2.3
     */
    public ChangeGranularity addPairOrNop(Key key, Value value);
    /**
     * Removes key-value pair from the lookup structure, or fails if not possible.
     * <p> When attempting to remove a key-value pair with zero multiplicity from a non-delta bucket type
     * ({@link MemoryType#SETS} or {@link MemoryType#MULTISETS}}), an {@link IllegalStateException} is thrown.
     * @return the granularity of the change
     * @throws IllegalStateException if removing non-existing element that is not permitted
     */
    public ChangeGranularity removePair(Key key, Value value);
    /**
     * Removes key-value pair from the lookup structure.
     * <p> When attempting to remove a key-value pair with zero multiplicity from a non-delta bucket type
     * ({@link MemoryType#SETS} or {@link MemoryType#MULTISETS}}),
     * the operation is silently ignored and {@link ChangeGranularity#DUPLICATE} is returned.
     * @return the granularity of the change
     * @throws IllegalStateException if removing non-existing element that is not permitted
     * @since 2.3
     */
    public ChangeGranularity removePairOrNop(Key key, Value value);

    /**
     * Updates multiplicity of key-value pair by a positive amount.
     *
     * <p> PRE: count > 0
     *
     * @return the granularity of the change
     * @throws IllegalStateException if addition would cause duplication that is not permitted
     */
    public ChangeGranularity addPairPositiveMultiplicity(Key key, Value value, int count);

    /**
     * Empties out the lookup structure.
     */
    public void clear();

    /**
     * Provides semantic equality comparison.
     */
    public static <Key, Value> boolean equals(IMultiLookup<Key, Value> self, Object obj) {
        if (obj instanceof IMultiLookup<?, ?>) {
            IMultiLookup<?, ?> other = (IMultiLookup<?, ?>) obj;
            if (other.countKeys() != self.countKeys()) return false;
            for (Object key : other.distinctKeys()) {
                if (! other.lookupUnsafe(key).equals(self.lookupUnsafe(key)))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Provides semantic hashCode() comparison.
     */
    public static <Key, Value> int hashCode(IMultiLookup<Key, Value> memory) {
        int hashCode = 0;
        for (Key key : memory.distinctKeys()) {
            hashCode += key.hashCode() ^ memory.lookup(key).hashCode();
        }
        return hashCode;
    }

}
