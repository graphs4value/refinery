/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.tuple;

import java.util.Arrays;

/**
 * Default Tuple implementation, with statically unknown arity.
 * @author Gabor Bergmann
 */
public final class FlatTuple extends BaseFlatTuple {

    /**
     * Array of substituted values. DO NOT MODIFY! Use Constructor to build a new instance instead.
     */
    private final Object[] elements;

    /**
     * Creates a FlatTuple instance, fills it with the given array.
     * <p> Users should consider calling {@link Tuples#flatTupleOf(Object...)} instead to save memory on low-arity tuples.
     *
     * @param elements
     *            array of substitution values
     */
    protected FlatTuple(Object... elements) {
        this.elements = Arrays.copyOf(elements, elements.length);
        calcHash();
    }

    @Override
    public Object get(int index) {
        return elements[index];
    }

    @Override
    public int getSize() {
        return elements.length;
    }

    @Override
    public Object[] getElements() {
        return elements;
    }

    @Override
    protected boolean internalEquals(ITuple other) {
        if (other instanceof FlatTuple) {
            return Arrays.equals(elements, ((FlatTuple) other).elements);
        } else
            return super.internalEquals(other);
    }

}
