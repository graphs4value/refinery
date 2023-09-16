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
 * Flat tuple with statically known arity of 1.
 *
 * @author Gabor Bergmann
 * @since 1.7
 *
 */
public final class FlatTuple1 extends BaseFlatTuple {
    private final Object element0;

    protected FlatTuple1(Object element0) {
        this.element0 = element0;
        calcHash();
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Object get(int index) {
        if (index == 0) return element0;
        else throw raiseIndexingError(index);
    }

    @Override
    protected boolean internalEquals(ITuple other) {
        return 1 == other.getSize() &&
                Objects.equals(element0, other.get(0));
    }
}
