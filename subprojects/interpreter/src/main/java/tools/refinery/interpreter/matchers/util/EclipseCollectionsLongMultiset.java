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
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;

/**
 * @author Gabor Bergmann
 * @since 2.0
 * <p> TODO refactor common methods with {@link EclipseCollectionsMultiset}
 * <p> TODO refactor into LongBagMemory etc.
 */
public class EclipseCollectionsLongMultiset extends LongIntHashMap implements IMultiset<Long> {

    @Override
    public boolean addOne(Long value) {
        int oldCount = super.getIfAbsent(value, 0);

        super.put(value, oldCount + 1);

        return oldCount == 0;
    }

    @Override
    public boolean addSigned(Long value, int count) {
        int oldCount = super.getIfAbsent(value, 0);
        int newCount = oldCount + count;

        boolean becomesZero = newCount == 0;
        if (newCount < 0)
            throw new IllegalStateException(String.format(
                    "Cannot remove %d occurrences of value '%s' as only %d would remain in %s",
                    count, value, newCount, this));
        else if (becomesZero)
            super.removeKey(value);
        else // (newCount > 0)
            super.put(value, newCount);

        return becomesZero || oldCount == 0;
    }

    @Override
    public boolean removeOne(Long value) {
        return removeOneInternal(value, true);
    }
    /**
     * @since 2.3
     */
    @Override
    public boolean removeOneOrNop(Long value) {
        return removeOneInternal(value, false);
    }


    /**
     * @since 2.3
     */
    protected boolean removeOneInternal(Long value, boolean throwIfImpossible) {
        int oldCount = super.getIfAbsent(value, 0);
        if (oldCount == 0) {
            if (throwIfImpossible) throw new IllegalStateException(String.format(
                    "Cannot remove value '%s' that is not contained in %s",
                    value, this));
            else return false;
        }

        int rest = oldCount - 1;
        boolean empty = rest == 0;

        if (!empty) {
            super.put(value, rest);
        } else {
            super.remove(value);
        }

        return empty;
    }

    @Override
    public void clearAllOf(Long value) {
        super.remove(value);
    }

    @Override
    public int getCount(Long value) {
        return super.getIfAbsent(value, 0);
    }
    @Override
    public int getCountUnsafe(Object value) {
        return value instanceof Long ? getCount((Long) value) : 0;
    }

    @Override
    public boolean containsNonZero(Long value) {
        return super.containsKey(value);
    }

    @Override
    public boolean containsNonZeroUnsafe(Object value) {
        return value instanceof Long && containsNonZero((Long) value);
    }

    @Override
    public Iterator<Long> iterator() {
        return EclipseCollectionsLongSetMemory.iteratorOf(super.keySet());
    }

    @Override
    public boolean addPositive(Long value, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("The count value must be positive!");
        }

        int oldCount = super.getIfAbsent(value, 0);

        super.put(value, oldCount + count);

        return oldCount == 0;
    }

    @Override
    public Set<Long> distinctValues() {
        return new EclipseCollectionsLongSetMemory.SetWrapper(super.keySet());
    }

    @Override
    public void forEachEntryWithMultiplicities(BiConsumer<Long, Integer> entryConsumer) {
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
