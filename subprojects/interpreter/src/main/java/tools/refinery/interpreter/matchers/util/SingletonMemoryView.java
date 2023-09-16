/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An immutable memory view that consists of a single non-null element with multiplicity 1.
 * @author Gabor Bergmann
 * @since 2.0
 */
public final class SingletonMemoryView<Value> implements IMemoryView<Value> {

    private Value wrapped;
    private static final int ONE_HASH = Integer.valueOf(1).hashCode();

    public SingletonMemoryView(Value value) {
        this.wrapped = value;
    }

    @Override
    public Iterator<Value> iterator() {
        return new Iterator<Value>() {
            boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public Value next() {
                if (hasNext) {
                    hasNext = false;
                    return wrapped;
                } else throw new NoSuchElementException();
            }
        };
    }

    @Override
    public int getCount(Value value) {
        return wrapped.equals(value) ? 1 : 0;
    }

    @Override
    public int getCountUnsafe(Object value) {
        return wrapped.equals(value) ? 1 : 0;
    }

    @Override
    public boolean containsNonZero(Value value) {
        return wrapped.equals(value);
    }

    @Override
    public boolean containsNonZeroUnsafe(Object value) {
        return wrapped.equals(value);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<Value> distinctValues() {
        return Collections.singleton(wrapped);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IMemoryView<?>) {
            IMemoryView<?> other = (IMemoryView<?>) obj;
            if (1 != other.size()) return false;
            if (1 != other.getCountUnsafe(wrapped)) return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode() ^ ONE_HASH;
    }

    @Override
    public String toString() {
        return "{" + wrapped + "}";
    }
}
