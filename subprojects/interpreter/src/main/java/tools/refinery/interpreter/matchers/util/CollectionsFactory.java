/*******************************************************************************
 * Copyright (c) 2010-2013, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Factory class used as an accessor to Collections implementations.
 * @author istvanrath
 */
public final class CollectionsFactory
{

    /**
     * Instantiates a new empty map.
     * @since 1.7
     */
    public static <K, V> Map<K, V> createMap() {
        return FRAMEWORK.createMap();
    }

    /**
     * Instantiates a new map with the given initial contents.
     * @since 1.7
     */
    public static <K, V> Map<K, V> createMap(Map<K, V> initial) {
        return FRAMEWORK.createMap(initial);
    }

    /**
     * Instantiates a new tree map.
     * @since 2.3
     */
    public static <K, V> TreeMap<K, V> createTreeMap() {
        return FRAMEWORK.createTreeMap();
    }

    /**
     * Instantiates a new empty set.
     * @since 1.7
     */
    public static <E> Set<E> createSet() {
        return FRAMEWORK.createSet();
    }

    /**
     * Instantiates a new set with the given initial contents.
     * @since 1.7
     */
    public static <E> Set<E> createSet(Collection<E> initial) {
        return FRAMEWORK.createSet(initial);
    }

    /**
     * Instantiates an empty set; the key parameter is used to allow using this as a method reference as a
     * {@link Function}, e.g. in {@link Map#computeIfAbsent(Object, Function)}.
     *
     * @param key
     *            the value of this parameter is ignored
     * @since 2.0
     */
    public static <T> Set<T> emptySet(Object key) {
        return FRAMEWORK.createSet();
    }

    /**
     * Instantiates a new empty multiset.
     * @since 1.7
     */
    public static <T> IMultiset<T> createMultiset() {
        return FRAMEWORK.createMultiset();
    }

    /**
     * Instantiates an empty multiset; the key parameter is used to allow using this as a method reference as a
     * {@link Function}, e.g. in {@link Map#computeIfAbsent(Object, Function)}.
     *
     * @param key
     *            the value of this parameter is ignored
     * @since 2.0
     */
    public static <T> IMultiset<T> emptyMultiset(Object key) {
        return FRAMEWORK.createMultiset();
    }

    /**
     * Instantiates a new empty delta bag.
     * @since 1.7
     */
    public static <T> IDeltaBag<T> createDeltaBag() {
        return FRAMEWORK.createDeltaBag();
    }

    /**
     * Instantiates a new list that is optimized for registering observers / callbacks.
     * @since 1.7
     */
    public static <O> List<O> createObserverList() {
        return FRAMEWORK.createObserverList();
    }

    /**
     * Instantiates a size-optimized multimap from keys to sets of values.
     * <p>For a single key, many values can be associated according to the given bucket semantics.
     * <p>The keys and values are stored as type fromKeys resp. ofValues;
     *  currently Object.class and Long.class are supported.
     * @since 2.0
     */
    public static <K, V> IMultiLookup<K, V> createMultiLookup(
            Class<? super K> fromKeys, MemoryType toBuckets, Class<? super V> ofValues) {
        return FRAMEWORK.createMultiLookup(fromKeys, toBuckets, ofValues);
    }

    /**
     * Instantiates a memory storing values.
     * <p>For a single key, many values can be associated according to the given memory semantics.
     * <p>The values are stored as type 'values';
     *  currently Object.class and Long.class are supported.
     * @since 2.0
     */
    public static <T> IMemory<T> createMemory(
            Class<? super T> values, MemoryType memoryType) {
        return FRAMEWORK.createMemory(values, memoryType);
    }

   /**
    * The type of {@link IMemory}
     * @since 2.0
     * TODO add delta as type
     */
   public enum MemoryType {
       /**
        * A single key-value pair is stored at most once
        */
       SETS,
       /**
        * Duplicate key-value pairs allowed
        */
       MULTISETS
   }

    /**
     * The collections framework of the current configuration.
     * @since 1.7
     */
    private static final ICollectionsFramework FRAMEWORK = new EclipseCollectionsFactory();

    /**
     * Interface abstracting over a collections technology that provides custom collection implementations.
     * @since 1.7
     */
    public static interface ICollectionsFramework {

        public abstract <K,V> Map<K,V> createMap();
        public abstract <K,V> Map<K,V> createMap(Map<K,V> initial);
        /**
         * @since 2.3
         */
        public abstract <K, V> TreeMap<K, V> createTreeMap();
        public abstract <E> Set<E> createSet();
        public abstract <E> Set<E> createSet(Collection<E> initial);
        public abstract <T> IMultiset<T> createMultiset();
        public abstract <T> IDeltaBag<T> createDeltaBag();
        public abstract <O> List<O> createObserverList();

        /**
         * @since 2.0
         */
        public abstract <K, V> IMultiLookup<K, V> createMultiLookup(
                Class<? super K> fromKeys, MemoryType toBuckets, Class<? super V> ofValues);
        /**
         * @since 2.0
         */
        public abstract <T> IMemory<T> createMemory(Class<? super T> values, MemoryType memoryType);
    }

}
