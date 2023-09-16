/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import tools.refinery.interpreter.matchers.util.MarkedMemory.MarkedMultiset;
import tools.refinery.interpreter.matchers.util.MarkedMemory.MarkedSet;

import java.util.Set;
import java.util.stream.Stream;



/**
 * Eclipse Collections-based realizations of {@link IMultiLookup}
 *
 * @author Gabor Bergmann
 * @since 2.0
 */
class EclipseCollectionsMultiLookup {

    private EclipseCollectionsMultiLookup() {/* Hidden utility class constructor */}

    private static class MarkedSetImpl<Value> extends EclipseCollectionsSetMemory<Value> implements MarkedMemory.MarkedSet<Value> {}
    private static class MarkedMultisetImpl<Value> extends EclipseCollectionsMultiset<Value> implements MarkedMemory.MarkedMultiset<Value> {}
    private static class MarkedLongSetImpl extends EclipseCollectionsLongSetMemory implements MarkedMemory.MarkedSet<Long> {}
    private static class MarkedLongMultisetImpl extends EclipseCollectionsLongMultiset implements MarkedMemory.MarkedMultiset<Long> {}

    public abstract static class FromObjects<Key, Value, Bucket extends MarkedMemory<Value>>
        extends UnifiedMap<Key, Object> implements IMultiLookupAbstract<Key, Value, Bucket> {

        @Override
        public boolean equals(Object obj) {
            return IMultiLookup.equals(this, obj);
        }
        @Override
        public int hashCode() {
            return IMultiLookup.hashCode(this);
        }


        @Override
        public Object lowLevelPutIfAbsent(Key key, Value value) {
            return super.putIfAbsent(key, value);
        }

        @Override
        public Object lowLevelGet(Key key) {
            return super.get(key);
        }

        @Override
        public Object lowLevelGetUnsafe(Object key) {
            return super.get(key);
        }

        @Override
        public Object lowLevelRemove(Key key) {
            return super.remove(key);
        }

        @Override
        public void lowLevelPut(Key key, Object valueOrBucket) {
            super.put(key, valueOrBucket);
        }
        @Override
        public Iterable<Object> lowLevelValues() {
            return super.values();
        }
        @Override
        public Set<Key> lowLevelKeySet() {
            return super.keySet();
        }
        @Override
        public int lowLevelSize() {
            return super.size();
        }

        @Override
        public Stream<Key> distinctKeysStream() {
            // may be more efficient than the default spliterator
            return super.keySet().stream();
        }

        public abstract static class ToSets<Key, Value> extends FromObjects<Key, Value, MarkedSet<Value>>
            implements IMultiLookupAbstract.ToSetsAbstract<Key, Value>
        {
            public static class OfObjects<Key, Value> extends ToSets<Key, Value> {
                @Override
                public MarkedSet<Value> createMarkedSet() {
                    return new MarkedSetImpl<Value>();
                }
            }

            public static class OfLongs<Key> extends ToSets<Key, Long> {
                @Override
                public MarkedSet<Long> createMarkedSet() {
                    return new MarkedLongSetImpl();
                }
            }

        }

        public abstract static class ToMultisets<Key, Value> extends FromObjects<Key, Value, MarkedMultiset<Value>>
            implements IMultiLookupAbstract.ToMultisetsAbstract<Key, Value>
        {
            public static class OfObjects<Key, Value> extends ToMultisets<Key, Value> {
                @Override
                public MarkedMultiset<Value> createMarkedMultiset() {
                    return new MarkedMultisetImpl<Value>();
                }
            }

            public static class OfLongs<Key> extends ToMultisets<Key, Long> {
                @Override
                public MarkedMultiset<Long> createMarkedMultiset() {
                    return new MarkedLongMultisetImpl();
                }
            }

        }

    }

    public abstract static class FromLongs<Value, Bucket extends MarkedMemory<Value>>
    extends LongObjectHashMap<Object> implements IMultiLookupAbstract<Long, Value, Bucket> {

        @Override
        public boolean equals(Object obj) {
            return IMultiLookup.equals(this, obj);
        }
        @Override
        public int hashCode() {
            return IMultiLookup.hashCode(this);
        }

        @Override
        public Object lowLevelPutIfAbsent(Long key, Value value) {
            Object old = super.get(key);
            if (old == null) super.put(key, value);
            return old;
        }

        @Override
        public Object lowLevelGet(Long key) {
            return super.get(key);
        }

        @Override
        public Object lowLevelGetUnsafe(Object key) {
            return key instanceof Long ? super.get((Long)key) : null;
        }

        @Override
        public Object lowLevelRemove(Long key) {
            return super.remove(key);
        }

        @Override
        public void lowLevelPut(Long key, Object valueOrBucket) {
            super.put(key, valueOrBucket);
        }
        @Override
        public Iterable<Object> lowLevelValues() {
            return super.values();
        }
        @Override
        public int lowLevelSize() {
            return super.size();
        }
        @Override
        public Iterable<Long> lowLevelKeySet() {
            return () -> EclipseCollectionsLongSetMemory.iteratorOf(FromLongs.super.keysView());
        }

    public abstract static class ToSets<Value> extends FromLongs<Value, MarkedSet<Value>>
        implements IMultiLookupAbstract.ToSetsAbstract<Long, Value>
    {
        public static class OfObjects<Value> extends ToSets<Value> {
            @Override
            public MarkedSet<Value> createMarkedSet() {
                return new MarkedSetImpl<Value>();
            }
        }

        public static class OfLongs extends ToSets<Long> {
            @Override
            public MarkedSet<Long> createMarkedSet() {
                return new MarkedLongSetImpl();
            }
        }

    }

    public abstract static class ToMultisets<Value> extends FromLongs<Value, MarkedMultiset<Value>>
        implements IMultiLookupAbstract.ToMultisetsAbstract<Long, Value>
    {
        public static class OfObjects<Value> extends ToMultisets<Value> {
            @Override
            public MarkedMultiset<Value> createMarkedMultiset() {
                return new MarkedMultisetImpl<Value>();
            }
        }

        public static class OfLongs extends ToMultisets<Long> {
            @Override
            public MarkedMultiset<Long> createMarkedMultiset() {
                return new MarkedLongMultisetImpl();
            }
        }

    }

    }


}


