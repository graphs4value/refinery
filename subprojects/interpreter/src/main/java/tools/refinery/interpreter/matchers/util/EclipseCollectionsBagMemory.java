/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.util;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

/**
 * Eclipse Collections-based multiset for tuples. Can contain duplicate occurrences of the same matching.
 *
 * <p>Inherits Eclipse Collections' Object-to-Int primitive hashmap and counts the number of occurrences of each value.
 * Element is deleted if # of occurences drops to 0.
 *
 * @author Gabor Bergmann.
 * @since 1.7
 * @noreference
 */
public abstract class EclipseCollectionsBagMemory<T> extends ObjectIntHashMap<T> implements IMemory<T> {

    public EclipseCollectionsBagMemory() {
        super();
    }

    @Override
    public int getCount(T value) {
        return super.getIfAbsent(value, 0);
    }
    @Override
    public int getCountUnsafe(Object value) {
        return super.getIfAbsent(value, 0);
    }
    @Override
    public boolean containsNonZero(T value) {
        return super.containsKey(value);
    }
    @Override
    public boolean containsNonZeroUnsafe(Object value) {
        return super.containsKey(value);
    }

    @Override
    public void clearAllOf(T value) {
        super.remove(value);
    }


    @Override
    public Iterator<T> iterator() {
        return super.keySet().iterator();
    }

    @Override
    public String toString() {
        return "TM" + super.toString();
    }

    @Override
    public Set<T> distinctValues() {
        return super.keySet();
    }

    @Override
    public void forEachEntryWithMultiplicities(BiConsumer<T, Integer> entryConsumer) {
        super.forEachKeyValue(entryConsumer::accept);
    }

    @Override
    public int hashCode() {
        return IMemoryView.hashCode(this);
    }
    @Override
    public boolean equals(Object obj) {
        return IMemoryView.equals(this, obj);
    }

}
