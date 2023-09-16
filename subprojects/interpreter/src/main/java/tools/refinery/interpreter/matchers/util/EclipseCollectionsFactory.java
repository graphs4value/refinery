/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;

/**
 * @author Gabor Bergmann
 * @since 1.7
 * @noreference This class is not intended to be referenced by clients.
 */
public class EclipseCollectionsFactory implements CollectionsFactory.ICollectionsFramework {

    @Override
    public <K, V> Map<K, V> createMap() {
        return Maps.mutable.empty();
    }

    @Override
    public <K, V> Map<K, V> createMap(Map<K, V> initial) {
        MutableMap<K, V> result = Maps.mutable.ofInitialCapacity(initial.size());
        result.putAll(initial);
        return result;
    }

    @Override
    public <K, V> TreeMap<K, V> createTreeMap() {
        // eclipse collections is doing the same
        return new TreeMap<>();
    }

    @Override
    public <E> Set<E> createSet() {
        return Sets.mutable.empty();
    }

    @Override
    public <E> Set<E> createSet(Collection<E> initial) {
        return Sets.mutable.ofAll(initial);
    }

    @Override
    public <T> IMultiset<T> createMultiset() {
        return new EclipseCollectionsMultiset<T>();
    }

    @Override
    public <T> IDeltaBag<T> createDeltaBag() {
        return new EclipseCollectionsDeltaBag<T>();
    }

    @Override
    public <O> List<O> createObserverList() {
        return new ArrayList<O>(1); // keep concurrent modification exceptions for error detection
        // Lists.mutable.empty

    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <K, V> IMultiLookup<K, V> createMultiLookup(
            Class<? super K> fromKeys,
            CollectionsFactory.MemoryType toBuckets,
            Class<? super V> ofValues)
    {
        boolean longKeys = Long.class.equals(fromKeys);
        boolean objectKeys = Object.class.equals(fromKeys);
        if (! (longKeys || objectKeys)) throw new IllegalArgumentException(fromKeys.getName());
        boolean longValues = Long.class.equals(ofValues);
        boolean objectValues = Object.class.equals(ofValues);
        if (! (longValues || objectValues)) throw new IllegalArgumentException(ofValues.getName());

        if (longKeys) { // K == java.lang.Long
            if (longValues) { // V == java.lang.Long
                switch(toBuckets) {
                case MULTISETS:
                    return (IMultiLookup<K, V>) new EclipseCollectionsMultiLookup.FromLongs.ToMultisets.OfLongs();
                case SETS:
                    return (IMultiLookup<K, V>) new EclipseCollectionsMultiLookup.FromLongs.ToSets.OfLongs();
                default:
                    throw new IllegalArgumentException(toBuckets.toString());
                }
            } else { // objectValues
                switch(toBuckets) {
                case MULTISETS:
                    return new EclipseCollectionsMultiLookup.FromLongs.ToMultisets.OfObjects();
                case SETS:
                    return new EclipseCollectionsMultiLookup.FromLongs.ToSets.OfObjects();
                default:
                    throw new IllegalArgumentException(toBuckets.toString());
                }
            }
        } else { // objectKeys
            if (longValues) { // V == java.lang.Long
                switch(toBuckets) {
                case MULTISETS:
                    return new EclipseCollectionsMultiLookup.FromObjects.ToMultisets.OfLongs();
                case SETS:
                    return new EclipseCollectionsMultiLookup.FromObjects.ToSets.OfLongs();
                default:
                    throw new IllegalArgumentException(toBuckets.toString());
                }
            } else { // objectValues
                switch(toBuckets) {
                case MULTISETS:
                    return new EclipseCollectionsMultiLookup.FromObjects.ToMultisets.OfObjects();
                case SETS:
                    return new EclipseCollectionsMultiLookup.FromObjects.ToSets.OfObjects();
                default:
                    throw new IllegalArgumentException(toBuckets.toString());
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> IMemory<T> createMemory(Class<? super T> values, CollectionsFactory.MemoryType memoryType) {
        if (Long.class.equals(values)) { // T == java.lang.Long
            switch(memoryType) {
            case MULTISETS:
                return (IMemory<T>) new EclipseCollectionsLongMultiset();
            case SETS:
                return (IMemory<T>) new EclipseCollectionsLongSetMemory();
            default:
                throw new IllegalArgumentException(memoryType.toString());
            }
        } else { // objectValues
            switch(memoryType) {
            case MULTISETS:
                return new EclipseCollectionsMultiset<>();
            case SETS:
                return new EclipseCollectionsSetMemory<>();
            default:
                throw new IllegalArgumentException(memoryType.toString());
            }
        }
    }



}
