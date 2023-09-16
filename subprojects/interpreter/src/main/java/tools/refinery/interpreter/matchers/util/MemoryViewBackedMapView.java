/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A partial and read-only Map implementation, mapping elements to multiplicities backed by an {@link IMemoryView}.
 *
 * <p> Not implemented: write methods.
 *
 * <p> Inefficiently implemented: {@link #containsValue(Object)}, {@link #values()}, {@link #entrySet()}.
 *
 * @author Gabor Bergmann
 * @since 2.0
 */
public class MemoryViewBackedMapView<T> implements Map<T, Integer> {

    private static final String READ_ONLY = "Read only";
    private final IMemoryView<T> wrapped;

    /**
     * @param wrapped a memory view whose contents are to be exposed as an element-to-integer map.
     */
    protected MemoryViewBackedMapView(IMemoryView<T> wrapped) {
        super();
        this.wrapped = wrapped;
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return wrapped.containsNonZeroUnsafe(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof Integer) {
            for (Entry<T, Integer> entry : wrapped.entriesWithMultiplicities()) {
                if (entry.getValue().equals(value)) return true;
            }
        }
        return false;
    }

    @Override
    public Integer put(T key, Integer value) {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public Integer get(Object key) {
        int count = wrapped.getCountUnsafe(key);
        if (count == 0) return null; else return count;
    }

    @Override
    public Integer remove(Object key) {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public void putAll(Map<? extends T, ? extends Integer> m) {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public Set<T> keySet() {
        return wrapped.distinctValues();
    }

    @Override
    public Collection<Integer> values() {
        Collection<Integer> result = new ArrayList<>();
        wrapped.forEachEntryWithMultiplicities((value, count) -> result.add(count));
        return result;
    }

    @Override
    public Set<Entry<T, Integer>> entrySet() {
        Set<Entry<T, Integer>> result = new HashSet<>();
        for (Entry<T, Integer> entry : wrapped.entriesWithMultiplicities()) {
            result.add(entry);
        }
        return result;
    }


    @Override
    public String toString() {
        return wrapped.toString();
    }
}
