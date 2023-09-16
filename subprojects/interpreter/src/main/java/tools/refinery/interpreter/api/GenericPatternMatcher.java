/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api;

import tools.refinery.interpreter.api.impl.BaseMatcher;
import tools.refinery.interpreter.api.scope.QueryScope;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * This is a generic pattern matcher for any Refinery Interpreter pattern, with "interpretative" query execution.
 * To use the pattern matcher on a given model, obtain a {@link GenericQuerySpecification} first, then
 * invoke e.g. {@link GenericQuerySpecification#getMatcher(InterpreterEngine)}.
 * in conjunction with {@link InterpreterEngine#on(QueryScope)}.
 * <p>
 * Whenever available, consider using the pattern-specific generated matcher API instead.
 *
 * <p>
 * Matches of the pattern will be represented as {@link GenericPatternMatch}.
 *
 * @author Bergmann Gábor
 * @see GenericPatternMatch
 * @see GenericMatchProcessor
 * @see GenericQuerySpecification
 * @since 0.9
 */
public class GenericPatternMatcher extends BaseMatcher<GenericPatternMatch> {

    /**
     * @since 1.4
     */
    public GenericPatternMatcher(GenericQuerySpecification<? extends GenericPatternMatcher> specification) {
        super(specification);
    }

    @Override
    public GenericPatternMatch arrayToMatch(Object[] parameters) {
        return GenericPatternMatch.newMatch(getSpecification(), parameters);
    }

    @Override
    public GenericPatternMatch arrayToMatchMutable(Object[] parameters) {
        return GenericPatternMatch.newMutableMatch(getSpecification(), parameters);
    }

    @Override
    protected GenericPatternMatch tupleToMatch(Tuple t) {
        return new GenericPatternMatch.Immutable(getSpecification(), /*avoid re-cloning*/t.getElements());
    }

    @SuppressWarnings("unchecked")
    @Override
    public GenericQuerySpecification<? extends GenericPatternMatcher> getSpecification() {
        return (GenericQuerySpecification<? extends GenericPatternMatcher>)querySpecification;
    }

    /**
     * Internal method for {@link GenericQuerySpecification}
     * @noreference
     */
    static <Matcher extends GenericPatternMatcher> GenericPatternMatcher instantiate(GenericQuerySpecification<Matcher> querySpecification) {
        return new GenericPatternMatcher(querySpecification);
    }

    /**
     * Internal method for {@link GenericQuerySpecification}
     * @noreference
     */
    static <Matcher extends GenericPatternMatcher> GenericPatternMatcher instantiate(InterpreterEngine engine, GenericQuerySpecification<Matcher> querySpecification) {
        // check if matcher already exists
        GenericPatternMatcher matcher = engine.getExistingMatcher(querySpecification);
        if (matcher == null) {
            matcher = engine.getMatcher(querySpecification);
        }
        return matcher;
    }

}
