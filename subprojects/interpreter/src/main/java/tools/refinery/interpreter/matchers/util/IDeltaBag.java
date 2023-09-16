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
 * An {@link IMemory} that represents the difference between two states of a set or {@link IMultiset}, and therefore
 *  may contain values with a negative multiplicity.
 *
 * @author Gabor Bergmann
 * @since 1.7
 */
public interface IDeltaBag<T> extends IMemory<T> {

    @Override
    default boolean removeOneOrNop(T value) {
        // makes no difference for delta bags
        return removeOne(value);
    }

}
