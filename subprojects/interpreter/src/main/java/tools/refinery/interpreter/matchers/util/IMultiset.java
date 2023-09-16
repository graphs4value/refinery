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
 * An {@link IMemory} that always contains values with a nonnegative multiplicity.
 *
 * <p> In case a write operation caused underflow, an {@link IllegalStateException} is thrown.
 *
 * @author Gabor Bergmann
 * @since 1.7
 */
public interface IMultiset<T> extends IMemory<T> {

    /**
     * Adds the given number of occurrences to the memory. The count value must be a positive number.
     *
     * @param count
     *            the number of occurrences
     * @return true if the tuple was not present before in the memory
     */
    boolean addPositive(T value, int count);

}
