/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.scope;

import tools.refinery.interpreter.internal.apiimpl.EngineContextFactory;
import tools.refinery.interpreter.api.IQuerySpecification;
import tools.refinery.interpreter.api.InterpreterEngine;

/**
 * Defines a scope for a Refinery Interpreter engine, which determines the set of model elements that query evaluation
 * operates on.
 *
 * @author Bergmann Gabor
 *
 */
public abstract class QueryScope extends EngineContextFactory {

    /**
     * Determines whether a query engine initialized on this scope can evaluate queries formulated against the given scope type.
     * <p> Every query scope class is compatible with a query engine initialized on a scope of the same class or a subclass.
     * @param queryScopeClass the scope class returned by invoking {@link IQuerySpecification#getPreferredScopeClass()} on a query specification
     * @return true if an {@link InterpreterEngine} initialized on this scope can consume an {@link IQuerySpecification}
     */
    public boolean isCompatibleWithQueryScope(Class<? extends QueryScope> queryScopeClass) {
        return queryScopeClass.isAssignableFrom(this.getClass());
    }

}
