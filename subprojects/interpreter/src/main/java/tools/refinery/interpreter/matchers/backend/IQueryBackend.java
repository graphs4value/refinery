/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;

/**
 * Internal interface for a Refienry Interpreter query specification. Each query is associated with a pattern. Methods
 * instantiate a matcher of the pattern with various parameters.
 *
 * @author Bergmann Gábor
 * @since 0.9
 * @noextend This interface is not intended to be extended by users of the Refinery Interpreter framework, and should
 * only be used by the query engine
 */
public interface IQueryBackend {

	/**
	 * @return true iff this backend is incremental, i.e. it caches the results of queries for quick retrieval,
	 * and can provide update notifications on result set changes.
	 */
	public boolean isCaching();

    /**
     * Returns a result provider for a given query. Repeated calls may return the same instance.
     * @throws InterpreterRuntimeException
     */
	public IQueryResultProvider getResultProvider(PQuery query);

	/**
     * Returns a result provider for a given query. Repeated calls may return the same instance.
     * @param optional hints that may override engine and query defaults (as provided by {@link IQueryBackendHintProvider}). Can be null.
     * @throws InterpreterRuntimeException
	 * @since 1.4
     */
    public IQueryResultProvider getResultProvider(PQuery query, QueryEvaluationHint hints);

    /**
     * Returns an existing result provider for a given query, if it was previously constructed, returns null otherwise.
     * Will not construct and initialize new result providers.
     */
	public IQueryResultProvider peekExistingResultProvider(PQuery query);

	/**
	 * Propagates all pending updates in this query backend. The implementation of this method is optional, and it
	 * can be ignored entirely if the backend does not delay updates.
	 * @since 1.6
	 */
	public void flushUpdates();

	/**
	 * Disposes the query backend.
	 */
	public abstract void dispose();

	/**
	 * @return the factory that created this backend, if this functionality is supported
	 * @since 2.1
	 */
	public IQueryBackendFactory getFactory();

}
