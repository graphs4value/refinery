/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
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

/**
 * Wraps a Map<T, Integer> (mapping elements to non-zero multiplicities) into an {@link IMemoryView}.
 *
 * @author Gabor Bergmann
 * @since 2.0
 */
public class MapBackedMemoryView<T> implements IMemoryView<T> {

    private Map<T, Integer> wrapped;

    /**
     * @param wrapped an equivalent map from contained objects to multiplicities
     */
    protected MapBackedMemoryView(Map<T, Integer> wrapped) {
        super();
        this.wrapped = wrapped;
    }

    @Override
    public Iterator<T> iterator() {
        return wrapped.keySet().iterator();
    }

    @Override
    public int getCount(T value) {
        return getCountUnsafe(value);
    }

    @Override
    public int getCountUnsafe(Object value) {
        Integer count = wrapped.get(value);
        return count == null ? 0 : count;
    }

    @Override
    public boolean containsNonZero(T value) {
        return wrapped.containsKey(value);
    }

    @Override
    public boolean containsNonZeroUnsafe(Object value) {
        return wrapped.containsKey(value);
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
    public Set<T> distinctValues() {
        return wrapped.keySet();
    }


    @Override
    public void forEachEntryWithMultiplicities(BiConsumer<T, Integer> entryConsumer) {
        for (Entry<T, Integer> entry : wrapped.entrySet()) {
            entryConsumer.accept(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Iterable<Entry<T, Integer>> entriesWithMultiplicities() {
        return wrapped.entrySet();
    }

    @Override
    public int hashCode() {
        return IMemoryView.hashCode(this);
    }
    @Override
    public boolean equals(Object obj) {
        return IMemoryView.equals(this, obj);
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }
}
