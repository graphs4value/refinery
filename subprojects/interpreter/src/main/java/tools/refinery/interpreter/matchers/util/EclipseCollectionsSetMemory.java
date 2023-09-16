/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.Set;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

/**
 * @author Gabor Bergmann
 * @since 2.0
 */
public class EclipseCollectionsSetMemory<Value> extends UnifiedSet<Value> implements ISetMemory<Value> {
    @Override
    public int getCount(Value value) {
        return super.contains(value) ? 1 : 0;
    }
    @Override
    public int getCountUnsafe(Object value) {
        return super.contains(value) ? 1 : 0;
    }
    @Override
    public boolean containsNonZero(Value value) {
        return super.contains(value);
    }

    @Override
    public boolean containsNonZeroUnsafe(Object value) {
        return super.contains(value);
    }

    @Override
    public boolean addOne(Value value) {
        return super.add(value);
    }

    @Override
    public boolean addSigned(Value value, int count) {
        if (count == 1) return addOne(value);
        else if (count == -1) return removeOne(value);
        else throw new IllegalStateException();
    }

    @Override
    public boolean removeOne(Value value) {
        // Kept for binary compatibility
        return ISetMemory.super.removeOne(value);
    }

    @Override
    public boolean removeOneOrNop(Value value) {
        return super.remove(value);
    }

    @Override
    public void clearAllOf(Value value) {
        super.remove(value);
    }

    @Override
    public Set<Value> distinctValues() {
        return this;
    }

    @Override
    public Value theContainedVersionOf(Value value) {
        return super.get(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Value theContainedVersionOfUnsafe(Object value) {
        if (super.contains(value))
            return super.get((Value)value);
        else return null;
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
