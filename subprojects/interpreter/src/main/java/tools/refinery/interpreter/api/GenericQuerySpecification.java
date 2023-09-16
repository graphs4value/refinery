/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api;

import tools.refinery.interpreter.api.impl.BaseQuerySpecification;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.queries.PVisibility;

/**
 * This is a generic query specification for Refinery Interpreter pattern matchers, for "interpretative" query
 * execution. Should be subclassed by query specification implementations specific to query languages.
 *
 * <p>
 * When available, consider using the pattern-specific generated matcher API instead.
 *
 * <p>
 * The created matcher will be of type {@link GenericPatternMatcher}. Matches of the pattern will be represented as
 * {@link GenericPatternMatch}.
 *
 * <p>
 * Note for overriding (if you have your own query language or ):
 * Derived classes should use {@link #defaultInstantiate(InterpreterEngine)} for implementing
 * {@link #instantiate(InterpreterEngine)} if they use {@link GenericPatternMatcher} proper.
 *
 * @see GenericPatternMatcher
 * @see GenericPatternMatch
 * @author Bergmann Gábor
 * @noinstantiate This class is not intended to be instantiated by end-users.
 * @since 0.9
 */
public abstract class GenericQuerySpecification<Matcher extends GenericPatternMatcher> extends
        BaseQuerySpecification<Matcher> {

    /**
     * Instantiates query specification for the given internal query representation.
     */
    public GenericQuerySpecification(PQuery wrappedPQuery) {
        super(wrappedPQuery);
    }

    @Override
    public GenericPatternMatch newEmptyMatch() {
        return GenericPatternMatch.newEmptyMatch(this);
    }

    @Override
    public GenericPatternMatch newMatch(Object... parameters) {
        return GenericPatternMatch.newMatch(this, parameters);
    }

    /**
     * Derived classes should use this implementation of {@link #instantiate(InterpreterEngine)}
     * if they use {@link GenericPatternMatcher} proper.
     * @throws InterpreterRuntimeException
     */
    protected GenericPatternMatcher defaultInstantiate(InterpreterEngine engine) {
        return GenericPatternMatcher.instantiate(engine, this);
    }

    /**
     * @since 2.0
     */
    @Override
    public PVisibility getVisibility() {
        return getInternalQueryRepresentation().getVisibility();
    }

}
