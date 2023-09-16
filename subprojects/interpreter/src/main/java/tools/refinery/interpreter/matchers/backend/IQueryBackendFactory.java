/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * A Query Backend Factory identifies a query evaluator implementation, and can create an evaluator instance (an
 * {@link IQueryBackend}) tied to a specific Refinery Interpreter engine upon request.
 *
 * <p> The factory is used as a lookup key for the backend instance,
 *   therefore implementors should either be singletons, or implement equals() / hashCode() accordingly.
 *
 * @author Bergmann Gabor
 *
 */
public interface IQueryBackendFactory {

    /**
     * Creates a new {@link IQueryBackend} instance tied to the given context elements.
     *
     * @return an instance of the class returned by {@link #getBackendClass()} that operates in the given context.
     * @since 1.5
     */
    public IQueryBackend
        create(IQueryBackendContext context);


    /**
     * The backend instances created by this factory are guaranteed to conform to the returned class.
     */
    public Class<? extends IQueryBackend> getBackendClass();

    /**
     * Calculate the required capabilities, which are needed to execute the given pattern
     *
     * @since 1.4
     */
    public IMatcherCapability calculateRequiredCapability(PQuery query, QueryEvaluationHint hint);

    /**
     * Returns whether the current backend is caching
     * @since 2.0
     */
    public boolean isCaching();
}
