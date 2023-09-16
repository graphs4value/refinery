/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Interface for a Refinery Interpreter matcher associated with a graph pattern.
 *
 * @param <Match>
 *            the IPatternMatch type representing a single match of this pattern.
 * @author Bergmann Gábor
 * @noimplement This interface is not intended to be implemented by clients. Implement BaseMatcher instead.
 */
public interface InterpreterMatcher<Match extends IPatternMatch> {
    // REFLECTION
    /** The pattern that will be matched. */
    IQuerySpecification<? extends InterpreterMatcher<Match>> getSpecification();

    /** Fully qualified name of the pattern. */
    String getPatternName();

    /** Returns the index of the symbolic parameter with the given name. */
    Integer getPositionOfParameter(String parameterName);

    /** Returns the array of symbolic parameter names. */
    List<String> getParameterNames();

    // ALL MATCHES
    /**
     * Returns the set of all pattern matches.
     *
     * @return matches represented as a Match object.
     */
    Collection<Match> getAllMatches();

    /**
     * Returns the set of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param partialMatch
     *            a partial match of the pattern where each non-null field binds the corresponding pattern parameter to
     *            a fixed value.
     * @return matches represented as a Match object.
     */
    Collection<Match> getAllMatches(Match partialMatch);

    /**
     * Returns a stream of all pattern matches.
     * <p>
     * <strong>WARNING</strong> If the result set changes while the stream is evaluated, the set of matches included in
     * the stream are unspecified. In such cases, either rely on {@link #getAllMatches()} or collect the results of the
     * stream in end-user code.
     *
     * @return matches represented as a Match object.
     * @since 2.0
     */
    Stream<Match> streamAllMatches();

    /**
     * Returns a stream of all matches of the pattern that conform to the given fixed values of some parameters.
     * <p>
     * <strong>WARNING</strong> If the result set changes while the stream is evaluated, the set of matches included in
     * the stream are unspecified. In such cases, either rely on {@link #getAllMatches()} or collect the results of the
     * stream in end-user code.
     *
     * @param partialMatch
     *            a partial match of the pattern where each non-null field binds the corresponding pattern parameter to
     *            a fixed value.
     * @return matches represented as a Match object.
     * @since 2.0
     */
    Stream<Match> streamAllMatches(Match partialMatch);

    // variant(s) with input binding as pattern-specific parameters: not declared in interface

    // SINGLE MATCH
    /**
     * Returns an arbitrarily chosen pattern match. Neither determinism nor randomness of selection is guaranteed.
     *
     * @return a match represented as a Match object, or an empty Optional if no match is found.
     * @since 2.0
     */
    Optional<Match> getOneArbitraryMatch();

    /**
     * Returns an arbitrarily chosen match of the pattern that conforms to the given fixed values of some parameters.
     * Neither determinism nor randomness of selection is guaranteed.
     *
     * @param partialMatch
     *            a partial match of the pattern where each non-null field binds the corresponding pattern parameter to
     *            a fixed value.
     * @return a match represented as a Match object, or an empty Optional if no match is found.
     * @since 2.0
     */
    Optional<Match> getOneArbitraryMatch(Match partialMatch);

    // variant(s) with input binding as pattern-specific parameters: not declared in interface

    // MATCH CHECKING
    /**
     * Indicates whether the query has any kind of matches.
     *
     * @return true if there exists a valid match of the pattern.
     * @since 1.7
     */
    boolean hasMatch();

    /**
     * Indicates whether the given combination of specified pattern parameters constitute a valid pattern match, under
     * any possible substitution of the unspecified parameters (if any).
     *
     * @param partialMatch
     *            a (partial) match of the pattern where each non-null field binds the corresponding pattern parameter
     *            to a fixed value.
     * @return true if the input is a valid (partial) match of the pattern.
     */
    boolean hasMatch(Match partialMatch);

    // variant(s) with input binding as pattern-specific parameters: not declared in interface

    // NUMBER OF MATCHES
    /**
     * Returns the number of all pattern matches.
     *
     * @return the number of pattern matches found.
     */
    int countMatches();

    /**
     * Returns the number of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param partialMatch
     *            a partial match of the pattern where each non-null field binds the corresponding pattern parameter to
     *            a fixed value.
     * @return the number of pattern matches found.
     */
    int countMatches(Match partialMatch);

    // variant(s) with input binding as pattern-specific parameters: not declared in interface

    // FOR EACH MATCH
    /**
     * Executes the given processor on each match of the pattern.
     *
     * @param processor
     *            the action that will process each pattern match.
     * @since 2.0
     */
    void forEachMatch(Consumer<? super Match> processor);

    /**
     * Executes the given processor on each match of the pattern that conforms to the given fixed values of some
     * parameters.
     *
     * @param partialMatch
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @param processor
     *            the action that will process each pattern match.
     * @since 2.0
     */
    void forEachMatch(Match partialMatch, Consumer<? super Match> processor);

    // variant(s) with input binding as pattern-specific parameters: not declared in interface

    // FOR ONE ARBITRARY MATCH
    /**
     * Executes the given processor on an arbitrarily chosen match of the pattern. Neither determinism nor randomness of
     * selection is guaranteed.
     *
     * @param processor
     *            the action that will process the selected match.
     * @return true if the pattern has at least one match, false if the processor was not invoked
     * @since 2.0
     */
    boolean forOneArbitraryMatch(Consumer<? super Match> processor);

    /**
     * Executes the given processor on an arbitrarily chosen match of the pattern that conforms to the given fixed
     * values of some parameters. Neither determinism nor randomness of selection is guaranteed.
     *
     * @param partialMatch
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @param processor
     *            the action that will process the selected match.
     * @return true if the pattern has at least one match with the given parameter values, false if the processor was
     *         not invoked
     * @since 2.0
     */
    boolean forOneArbitraryMatch(Match partialMatch, Consumer<? super Match> processor);

    // variant(s) with input binding as pattern-specific parameters: not declared in interface

    /**
     * Returns an empty, mutable Match for the matcher.
     * Fields of the mutable match can be filled to create a partial match, usable as matcher input.
     * This can be used to call the matcher with a partial match
     *  even if the specific class of the matcher or the match is unknown.
     *
     * @return the empty match
     */
    Match newEmptyMatch();

    /**
     * Returns a new (partial) Match object for the matcher.
     * This can be used e.g. to call the matcher with a partial
     * match.
     *
     * <p>The returned match will be immutable. Use {@link #newEmptyMatch()} to obtain a mutable match object.
     *
     * @param parameters
     *            the fixed value of pattern parameters, or null if not bound.
     * @return the (partial) match object.
     */
    Match newMatch(Object... parameters);

    /**
     * Retrieve the set of values that occur in matches for the given parameterName.
     *
     * @param parameterName
     *            name of the parameter for which values are returned
     * @return the Set of all values for the given parameter, null if the parameter with the given name does not exists,
     *         empty set if there are no matches
     */
    Set<Object> getAllValues(final String parameterName);

    /**
     * Retrieve the set of values that occur in matches for the given parameterName, that conforms to the given fixed
     * values of some parameters.
     *
     * @param parameterName
     *            name of the parameter for which values are returned
     * @param partialMatch
     *            a partial match of the pattern where each non-null field binds the corresponding pattern parameter to
     *            a fixed value.
     * @return the Set of all values for the given parameter, null if the parameter with the given name does not exists
     *         or if the parameter with the given name is set in partialMatch, empty set if there are no matches
     */
    Set<Object> getAllValues(final String parameterName, Match partialMatch);

    /**
     * Returns the engine that the matcher uses.
     *
     * @return the engine
     */
    InterpreterEngine getEngine();
}
