/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.refinery.interpreter.internal.apiimpl.QueryResultWrapper;
import tools.refinery.interpreter.matchers.backend.IMatcherCapability;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.util.Preconditions;
import tools.refinery.interpreter.api.IPatternMatch;
import tools.refinery.interpreter.api.IQuerySpecification;
import tools.refinery.interpreter.api.InterpreterEngine;
import tools.refinery.interpreter.api.InterpreterMatcher;

/**
 * Base implementation of ViatraQueryMatcher.
 *
 * @author Bergmann Gábor
 *
 * @param <Match>
 */
public abstract class BaseMatcher<Match extends IPatternMatch> extends QueryResultWrapper implements InterpreterMatcher<Match> {

    // FIELDS AND CONSTRUCTOR

    protected InterpreterEngine engine;
    protected IQuerySpecification<? extends BaseMatcher<Match>> querySpecification;
    private IMatcherCapability capabilities;

    /**
     * @since 1.4
     */
    public BaseMatcher(IQuerySpecification<? extends BaseMatcher<Match>> querySpecification) {
        this.querySpecification = querySpecification;
        this.querySpecification.getInternalQueryRepresentation().ensureInitialized();
    }

    /**
     * @since 1.4
     */
    @Override
    protected
    void setBackend(InterpreterEngine engine, IQueryResultProvider resultProvider, IMatcherCapability capabilities){
        this.backend = resultProvider;
        this.engine = engine;
        this.capabilities = capabilities;
    }

    // ARRAY-BASED INTERFACE

    /** Converts the array representation of a pattern match to an immutable Match object. */
    protected abstract Match arrayToMatch(Object[] parameters);
    /** Converts the array representation of a pattern match to a mutable Match object. */
    protected abstract Match arrayToMatchMutable(Object[] parameters);

    /** Converts the Match object of a pattern match to the array representation. */
    protected Object[] matchToArray(Match partialMatch) {
        return partialMatch.toArray();
    }
    // TODO make me public for performance reasons
    protected abstract Match tupleToMatch(Tuple t);

    private Object[] fEmptyArray;

    protected Object[] emptyArray() {
        if (fEmptyArray == null)
            fEmptyArray = new Object[getSpecification().getParameterNames().size()];
        return fEmptyArray;
    }

    // REFLECTION

    @Override
    public Integer getPositionOfParameter(String parameterName) {
        return getSpecification().getPositionOfParameter(parameterName);
    }

    @Override
    public List<String> getParameterNames() {
        return getSpecification().getParameterNames();
    }

    // BASE IMPLEMENTATION

    @Override
    public Collection<Match> getAllMatches() {
        return rawStreamAllMatches(emptyArray()).collect(Collectors.toSet());
    }

    @Override
    public Stream<Match> streamAllMatches() {
        return rawStreamAllMatches(emptyArray());
    }

    /**
     * Returns a stream of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @return matches represented as a Match object.
     * @since 2.0
     */
    protected Stream<Match> rawStreamAllMatches(Object[] parameters) {
        // clones the tuples into a match object to protect the Tuples from modifications outside of the ReteMatcher
        return backend.getAllMatches(parameters).map(this::tupleToMatch);
    }

    @Override
    public Collection<Match> getAllMatches(Match partialMatch) {
        return rawStreamAllMatches(partialMatch.toArray()).collect(Collectors.toSet());
    }

    @Override
    public Stream<Match> streamAllMatches(Match partialMatch) {
        return rawStreamAllMatches(partialMatch.toArray());
    }

    // with input binding as pattern-specific parameters: not declared in interface

    @Override
    public Optional<Match> getOneArbitraryMatch() {
        return rawGetOneArbitraryMatch(emptyArray());
    }

    /**
     * Returns an arbitrarily chosen match of the pattern that conforms to the given fixed values of some parameters.
     * Neither determinism nor randomness of selection is guaranteed.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @return a match represented as a Match object, or null if no match is found.
     * @since 2.0
     */
    protected Optional<Match> rawGetOneArbitraryMatch(Object[] parameters) {
        return backend.getOneArbitraryMatch(parameters).map(this::tupleToMatch);
    }

    @Override
    public Optional<Match> getOneArbitraryMatch(Match partialMatch) {
        return rawGetOneArbitraryMatch(partialMatch.toArray());
    }

    // with input binding as pattern-specific parameters: not declared in interface

    /**
     * Indicates whether the given combination of specified pattern parameters constitute a valid pattern match, under
     * any possible substitution of the unspecified parameters.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @return true if the input is a valid (partial) match of the pattern.
     */
    protected boolean rawHasMatch(Object[] parameters) {
        return backend.hasMatch(parameters);
    }

    @Override
    public boolean hasMatch() {
        return rawHasMatch(emptyArray());
    }

    @Override
    public boolean hasMatch(Match partialMatch) {
        return rawHasMatch(partialMatch.toArray());
    }

    // with input binding as pattern-specific parameters: not declared in interface

    @Override
    public int countMatches() {
        return rawCountMatches(emptyArray());
    }

    /**
     * Returns the number of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @return the number of pattern matches found.
     */
    protected int rawCountMatches(Object[] parameters) {
        return backend.countMatches(parameters);
    }

    @Override
    public int countMatches(Match partialMatch) {
        return rawCountMatches(partialMatch.toArray());
    }

    // with input binding as pattern-specific parameters: not declared in interface

    /**
     * Executes the given processor on each match of the pattern that conforms to the given fixed values of some
     * parameters.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @param action
     *            the action that will process each pattern match.
     * @since 2.0
     */
    protected void rawForEachMatch(Object[] parameters, Consumer<? super Match> processor) {
        backend.getAllMatches(parameters).map(this::tupleToMatch).forEach(processor);
    }

    @Override
    public void forEachMatch(Consumer<? super Match> processor) {
        rawForEachMatch(emptyArray(), processor);
    }

    @Override
    public void forEachMatch(Match match, Consumer<? super Match> processor) {
        rawForEachMatch(match.toArray(), processor);
    }

    // with input binding as pattern-specific parameters: not declared in interface

    @Override
    public boolean forOneArbitraryMatch(Consumer<? super Match> processor) {
        return rawForOneArbitraryMatch(emptyArray(), processor);
    }

    @Override
    public boolean forOneArbitraryMatch(Match partialMatch, Consumer<? super Match> processor) {
        return rawForOneArbitraryMatch(partialMatch.toArray(), processor);
    }

    /**
     * Executes the given processor on an arbitrarily chosen match of the pattern that conforms to the given fixed
     * values of some parameters. Neither determinism nor randomness of selection is guaranteed.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @param processor
     *            the action that will process the selected match.
     * @return true if the pattern has at least one match with the given parameter values, false if the processor was
     *         not invoked
     * @since 2.0
     */
    protected boolean rawForOneArbitraryMatch(Object[] parameters, Consumer<? super Match> processor) {
        return backend.getOneArbitraryMatch(parameters).map(this::tupleToMatch).map(m -> {
            processor.accept(m);
            return true;
        }).orElse(false);
    }

    // with input binding as pattern-specific parameters: not declared in interface


    @Override
    public Match newEmptyMatch() {
        return arrayToMatchMutable(new Object[getParameterNames().size()]);
    }

    @Override
    public Match newMatch(Object... parameters) {
        return arrayToMatch(parameters);
    }

    @Override
    public Set<Object> getAllValues(final String parameterName) {
        return rawStreamAllValues(getPositionOfParameter(parameterName), emptyArray()).collect(Collectors.toSet());
    }

    @Override
    public Set<Object> getAllValues(final String parameterName, Match partialMatch) {
        return rawStreamAllValues(getPositionOfParameter(parameterName), partialMatch.toArray()).collect(Collectors.toSet());
    }

    /**
     * Retrieve a stream of values that occur in matches for the given parameterName, that conforms to the given fixed
     * values of some parameters.
     *
     * @param position
     *            position of the parameter for which values are returned
     * @param parameters
     *            a parameter array corresponding to a partial match of the pattern where each non-null field binds the
     *            corresponding pattern parameter to a fixed value.
     * @return the stream of all values in the given position
     * @throws IllegalArgumentException
     *             if length of parameters array does not equal to number of parameters
     * @throws IndexOutOfBoundsException
     *             if position is not appropriate for the current parameter size
     * @since 2.0
     */
    protected Stream<Object> rawStreamAllValues(final int position, Object[] parameters) {
        Preconditions.checkElementIndex(position, getParameterNames().size());
        Preconditions.checkArgument(parameters.length == getParameterNames().size());
        return rawStreamAllMatches(parameters).map(match -> match.get(position));
    }

    /**
     * Uses an existing set to accumulate all values of the parameter with the given name. Since it is a protected
     * method, no error checking or input validation is performed!
     *
     * @param position
     *            position of the parameter for which values are returned
     * @param parameters
     *            a parameter array corresponding to a partial match of the pattern where each non-null field binds the
     *            corresponding pattern parameter to a fixed value.
     * @param accumulator
     *            the existing set to fill with the values
     */
    @SuppressWarnings("unchecked")
    protected <T> void rawAccumulateAllValues(final int position, Object[] parameters, final Set<T> accumulator) {
        rawForEachMatch(parameters, match -> accumulator.add((T) match.get(position)));
    }

    @Override
    public InterpreterEngine getEngine() {
        return engine;
    }

    @Override
    public IQuerySpecification<? extends BaseMatcher<Match>> getSpecification() {
        return querySpecification;
    }

    @Override
    public String getPatternName() {
        return querySpecification.getFullyQualifiedName();
    }

    /**
     * @since 1.4
     */
    public IMatcherCapability getCapabilities() {
        return capabilities;
    }
}
