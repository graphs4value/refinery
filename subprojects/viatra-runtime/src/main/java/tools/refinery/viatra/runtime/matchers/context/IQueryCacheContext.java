/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.context;

import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackend;
import tools.refinery.viatra.runtime.matchers.backend.IQueryResultProvider;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PQuery;

/**
 * Provides information on already cached queries to query evaluator backends at runtime.
 * 
 * @author Bergmann Gabor
 *
 */
public interface IQueryCacheContext {
    
    /**
     * Checks if there already is a caching result provider for the given query.
     * <p> Returns false if called while the caching result provider of the given query is being constructed in the first place. 
     */
    public boolean isResultCached(PQuery query);
    
    /**
     * Returns a caching result provider for the given query; it must be constructed if it does not exist yet.
     * <p> <b>Caution:</b> behavior undefined if called while the caching result provider of the given query is being constructed. 
     *   Beware of infinite loops. 
     * <p> <b>Postcondition:</b> {@link IQueryBackend#isCaching()} returns true for the {@link #getQueryBackend()} of the returned provider
     * 
     * @throws ViatraQueryRuntimeException
     */
    public IQueryResultProvider getCachingResultProvider(PQuery query);
}
