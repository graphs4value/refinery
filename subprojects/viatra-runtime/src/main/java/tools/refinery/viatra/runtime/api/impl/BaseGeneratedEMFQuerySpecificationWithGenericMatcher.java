/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.api.impl;

import tools.refinery.viatra.runtime.api.GenericPatternMatch;
import tools.refinery.viatra.runtime.api.GenericPatternMatcher;
import tools.refinery.viatra.runtime.api.GenericQuerySpecification;
import tools.refinery.viatra.runtime.api.ViatraQueryEngine;
import tools.refinery.viatra.runtime.api.scope.QueryScope;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery;

/**
 * Provides common functionality of pattern-specific generated query specifications for without generated
 * pattern-specific match and matcher classes, including private patterns.
 * 
 * @since 1.7
 *
 */
public abstract class BaseGeneratedEMFQuerySpecificationWithGenericMatcher
        extends GenericQuerySpecification<GenericPatternMatcher> {

    public BaseGeneratedEMFQuerySpecificationWithGenericMatcher(PQuery wrappedPQuery) {
        super(wrappedPQuery);
    }

    @Override
    public Class<? extends QueryScope> getPreferredScopeClass() {
        return EMFScope.class;
    }

    @Override
    protected GenericPatternMatcher instantiate(final ViatraQueryEngine engine) {
        return defaultInstantiate(engine);
    }

    @Override
    public GenericPatternMatcher instantiate() {
        return new GenericPatternMatcher(this);
    }

    @Override
    public GenericPatternMatch newEmptyMatch() {
        return GenericPatternMatch.newEmptyMatch(this);
    }

    @Override
    public GenericPatternMatch newMatch(final Object... parameters) {
        return GenericPatternMatch.newMatch(this, parameters);
    }

}