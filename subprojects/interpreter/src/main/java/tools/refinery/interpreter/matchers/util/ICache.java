/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.function.Supplier;

/**
 * A cache is a simple key-value pair that stores calculated values for specific key objects
 *
 * <p>
 * <b>NOTE</b> These caches are not expected to be used outside query backend implementations
 *
 * @author Zoltan Ujhelyi
 * @since 1.7
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface ICache {

    /**
     * Return a selected value for the key object. If the value is not available in the cache yet, the given provider is
     * called once
     * @since 2.0
     */
    <T> T getValue(Object key, Class<? extends T> clazz, Supplier<T> valueProvider);

}
