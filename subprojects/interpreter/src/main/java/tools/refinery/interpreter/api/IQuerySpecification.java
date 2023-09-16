/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api;

import tools.refinery.interpreter.api.scope.QueryScope;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.queries.PQueryHeader;

/**
 * API interface for a Refinery Interpreter query specification. Each query is associated with a pattern. Methods
 * instantiate a matcher of the pattern with various parameters.
 *
 * <p> As of 0.9.0, some internal details (mostly relevant for query evaluator backends) have been moved to {@link #getInternalQueryRepresentation()}.
 *
 * @author Bergmann Gábor
 *
 */
public interface IQuerySpecification<Matcher extends InterpreterMatcher<? extends IPatternMatch>> extends PQueryHeader {

    /**
     * Initializes the pattern matcher within an existing {@link InterpreterEngine}. If the pattern matcher is already
     * constructed in the engine, only a lightweight reference is created.
     * <p>
     * The match set will be incrementally refreshed upon updates.
     *
     * @param engine
     *            the existing Refinery Interpreter engine in which this matcher will be created.
     * @throws InterpreterRuntimeException
     *             if an error occurs during pattern matcher creation
     */
    public Matcher getMatcher(InterpreterEngine engine);


    /**
     * Returns an empty, mutable Match compatible with matchers of this query.
     * Fields of the mutable match can be filled to create a partial match, usable as matcher input.
     * This can be used to call the matcher with a partial match
     *  even if the specific class of the matcher or the match is unknown.
     *
     * @return the empty match
     */
    public abstract IPatternMatch newEmptyMatch();

    /**
     * Returns a new (partial) Match object compatible with matchers of this query.
     * This can be used e.g. to call the matcher with a partial
     * match.
     *
     * <p>The returned match will be immutable. Use {@link #newEmptyMatch()} to obtain a mutable match object.
     *
     * @param parameters
     *            the fixed value of pattern parameters, or null if not bound.
     * @return the (partial) match object.
     */
    public abstract IPatternMatch newMatch(Object... parameters);

    /**
     * The query is formulated over this kind of modeling platform.
     * E.g. for queries over EMF models, the {@link EMFScope} class is returned.
     */
    public Class<? extends QueryScope> getPreferredScopeClass();

    /**
     * Returns the definition of the query in a format intended for consumption by the query evaluator.
     * @return the internal representation of the query.
     */
    public PQuery getInternalQueryRepresentation();

    /**
     * Creates a new uninitialized matcher, which is not functional until an engine initializes it. Clients
     * should not call this method, it is used by the {@link InterpreterEngine} instance to instantiate matchers.
     * @throws InterpreterRuntimeException
     * @noreference This method is not intended to be referenced by clients.
     * @since 1.4
     */
    public Matcher instantiate();

}
