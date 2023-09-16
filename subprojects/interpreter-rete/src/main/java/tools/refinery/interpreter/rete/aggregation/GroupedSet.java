/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * An optimized {@link Set} implementation where each contained value is produced by combining together a grouping value
 * and some other (key) object. The way of combining together these two values is specified by the closure passed to the
 * constructor. Only a select few {@link Set} operations are supported. This collection is unmodifiable.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class GroupedSet<GroupingValueType, GroupedKeyType, WholeKeyType> implements Set<WholeKeyType> {

    protected final GroupingValueType group;
    protected final Collection<GroupedKeyType> values;
    protected final BiFunction<GroupingValueType, GroupedKeyType, WholeKeyType> valueFunc;

    public GroupedSet(final GroupingValueType group, final Collection<GroupedKeyType> values,
            final BiFunction<GroupingValueType, GroupedKeyType, WholeKeyType> valueFunc) {
        this.group = group;
        this.values = values;
        this.valueFunc = valueFunc;
    }

    @Override
    public int size() {
        return this.values.size();
    }

    @Override
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    @Override
    public boolean contains(final Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<WholeKeyType> iterator() {
        final Iterator<GroupedKeyType> wrapped = this.values.iterator();
        return new Iterator<WholeKeyType>() {
            @Override
            public boolean hasNext() {
                return wrapped.hasNext();
            }

            @Override
            public WholeKeyType next() {
                final GroupedKeyType value = wrapped.next();
                return valueFunc.apply(group, value);
            }
        };
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(final T[] arr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(final WholeKeyType tuple) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(final Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final Collection<? extends WholeKeyType> coll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection<?> coll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(final Collection<?> coll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}
