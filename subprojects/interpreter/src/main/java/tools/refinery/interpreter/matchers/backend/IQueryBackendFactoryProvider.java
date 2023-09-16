/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

/**
 * A provider interface for {@link IQueryBackendFactory} instances.
 * @since 2.0
 */
public interface IQueryBackendFactoryProvider {

    /**
     * Returns a query backend factory instance. The method should return the same instance in case of repeated calls.
     */
    IQueryBackendFactory getFactory();

    /**
     * Returns whether the given query backend should be considered as system default. If multiple backends are
     * registered as system default, it is undefined which one will be chosen.
     */
    default boolean isSystemDefaultEngine() {
        return false;
    }

    /**
     * Returns whether the given query backend should be considered as system default search backend. If multiple
     * backends are registered as system default, it is undefined which one will be chosen.
     */
    default boolean isSystemDefaultSearchBackend() {
        return false;
    }


    /**
     * Returns whether the given query backend should be considered as system default caching backend. If multiple
     * backends are registered as system default, it is undefined which one will be chosen.
     */
    default boolean isSystemDefaultCachingBackend() {
        return false;
    }
}
