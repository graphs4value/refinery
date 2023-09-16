/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, itemis AG, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.aggregation;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;

/**
 * An optimized {@link Map} implementation where each key is produced by joining together a group tuple and some other
 * object (via left inheritance). Only a select few {@link Map} operations are supported. This collection is
 * unmodifiable.
 *
 * Operations on this map assume that client queries also obey the contract that keys are constructed from a group tuple
 * and an additional object.
 *
 * @author Tamas Szabo
 * @since 2.4
 */
public class GroupedMap<GroupedKeyType, ValueType> implements Map<Tuple, ValueType> {

    protected final Tuple group;
    // cached group size value is to be used in get()
    private final int groupSize;
    protected final Map<GroupedKeyType, ValueType> mappings;
    protected final IQueryRuntimeContext runtimeContext;

    public GroupedMap(final Tuple group, final Map<GroupedKeyType, ValueType> mappings,
            final IQueryRuntimeContext runtimeContext) {
        this.group = group;
        this.groupSize = group.getSize();
        this.mappings = mappings;
        this.runtimeContext = runtimeContext;
    }

    @Override
    public int size() {
        return this.mappings.size();
    }

    @Override
    public boolean isEmpty() {
        return this.mappings.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(final Object value) {
        return this.mappings.containsValue(value);
    }

    @Override
    public ValueType get(final Object key) {
        if (key instanceof Tuple) {
            final Object value = ((Tuple) key).get(this.groupSize);
            final Object unwrappedValue = this.runtimeContext.unwrapElement(value);
            return this.mappings.get(unwrappedValue);
        } else {
            return null;
        }
    }

    @Override
    public ValueType put(final Tuple key, final ValueType value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueType remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(final Map<? extends Tuple, ? extends ValueType> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Tuple> keySet() {
        return new GroupedSet<Tuple, GroupedKeyType, Tuple>(this.group, this.mappings.keySet(), (g, v) -> {
            return Tuples.staticArityLeftInheritanceTupleOf(g, this.runtimeContext.wrapElement(v));
        });
    }

    @Override
    public Collection<ValueType> values() {
        return this.mappings.values();
    }

    @Override
    public Set<Entry<Tuple, ValueType>> entrySet() {
        return new GroupedSet<Tuple, GroupedKeyType, Entry<Tuple, ValueType>>(this.group, this.mappings.keySet(),
                (g, v) -> {
                    final Tuple key = Tuples.staticArityLeftInheritanceTupleOf(g, this.runtimeContext.wrapElement(v));
                    final ValueType value = this.mappings.get(v);
                    return new SimpleEntry<Tuple, ValueType>(key, value);
                });
    }

}
