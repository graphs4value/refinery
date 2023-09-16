/*******************************************************************************
 * Copyright (c) 2010-2017, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

/**
 * @author Gabor Bergmann
 * @since 1.7
 */
public class EclipseCollectionsDeltaBag<T> extends EclipseCollectionsBagMemory<T> implements IDeltaBag<T> {

    @Override
    public boolean addOne(T value) {
        return addSigned(value, +1);
    }

    @Override
    public boolean addSigned(T value, int count) {
        int oldCount = super.getIfAbsent(value, 0);
        int newCount = oldCount + count;

        boolean becomesZero = newCount == 0;
        if (becomesZero)
            super.removeKey(value);
        else
            super.put(value, newCount);

        return becomesZero || oldCount == 0;
    }


    @Override
    public boolean removeOne(T value) {
        return addSigned(value, -1);
    }
}
