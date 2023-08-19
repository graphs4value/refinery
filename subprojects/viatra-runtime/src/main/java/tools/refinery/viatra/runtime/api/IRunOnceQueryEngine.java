/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.api;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notifier;
import tools.refinery.viatra.runtime.base.api.BaseIndexOptions;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;

/**
 * A run-once query engine is used to get matches for queries without incremental support.
 * Users can create a query engine with a given {@link Notifier} as scope and use a query specification
 * to retrieve the current match set with this scope (see {@link #getAllMatches}).
 * 
 * @author Abel Hegedus
 * 
 */
public interface IRunOnceQueryEngine {

    /**
     * Returns the set of all matches for the given query in the scope of the engine.
     * 
     * @param querySpecification the query that is evaluated
     * @return matches represented as a Match object.
     */
    <Match extends IPatternMatch> Collection<Match> getAllMatches(
            final IQuerySpecification<? extends ViatraQueryMatcher<Match>> querySpecification);

    /**
     * @return the scope of pattern matching, i.e. the root of the EMF model tree that this engine is attached to.
     */
    Notifier getScope();
    
    /**
     * The base index options specifies how the base index is built, including wildcard mode (defaults to false) and
     * dynamic EMF mode (defaults to false). See {@link NavigationHelper} for the explanation of wildcard mode and
     * dynamic EMF mode.
     * 
     * <p/> The returned options can be modified in order to affect subsequent calls of {@link #getAllMatches}.
     * 
     * @return the base index options used by the engine. 
     */
    BaseIndexOptions getBaseIndexOptions(); 
    
    /**
     * When set to true, the run-once query engine will not dispose it's engine and will resample the values of derived
     * features before returning matches if the model changed since the last call.
     * 
     * If the values of derived features may change without any model modification, call {@link #resampleOnNextCall()}
     * before subsequent calls of {@link #getAllMatches}.
     * 
     * @param automaticResampling
     */
    void setAutomaticResampling(boolean automaticResampling);
    
    /**
     * If automatic resampling is enabled and the value of derived features may change without model modifications,
     * calling this method will make sure that re-sampling will occur before returning match results.
     */
    void resampleOnNextCall();
}
