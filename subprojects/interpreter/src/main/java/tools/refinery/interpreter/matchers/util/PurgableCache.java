/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 * @noreference This class is not intended to be referenced by clients.
 */
public class PurgableCache implements ICache {

    Map<Object, Object> storage = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(Object key, Class<? extends T> clazz, Supplier<T> valueProvider) {
        if (storage.containsKey(key)) {
            Object value = storage.get(key);
            Preconditions.checkState(clazz.isInstance(value), "Cache stores for key %s a value of %s that is incompatible with the requested type %s", key, value, clazz);
            return (T) value;
        } else {
            T value = valueProvider.get();
            storage.put(key, value);
            return value;
        }
    }

    /**
     * Removes all values stored in the cache
     */
    public void purge() {
        storage.clear();
    }
}
