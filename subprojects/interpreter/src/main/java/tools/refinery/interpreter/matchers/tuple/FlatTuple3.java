/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.tuple;

import java.util.Objects;

/**
 * Flat tuple with statically known arity of 3.
 *
 * @author Gabor Bergmann
 * @since 1.7
 *
 */
public final class FlatTuple3 extends BaseFlatTuple {
    private final Object element0;
    private final Object element1;
    private final Object element2;

    protected FlatTuple3(Object element0, Object element1, Object element2) {
        this.element0 = element0;
        this.element1 = element1;
        this.element2 = element2;
        calcHash();
    }

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public Object get(int index) {
        switch (index) {
        case 0: return element0;
        case 1: return element1;
        case 2: return element2;
        default: throw raiseIndexingError(index);
        }
    }

    @Override
    protected boolean internalEquals(ITuple other) {
        return 3 == other.getSize() &&
                Objects.equals(element0, other.get(0)) &&
                Objects.equals(element1, other.get(1)) &&
                Objects.equals(element2, other.get(2));
    }

}
