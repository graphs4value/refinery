/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.backend;

import java.util.Optional;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.planning.helpers.StatisticsHelper;
import tools.refinery.interpreter.matchers.tuple.ITuple;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Accuracy;

/**
 * An internal interface of the query backend that provides results of a given query.
 * @author Bergmann Gabor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IQueryResultProvider {

    /**
     * Decides whether there are any matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @since 2.0
     */
    public boolean hasMatch(Object[] parameters);

    /**
     * Decides whether there are any matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameterSeedMask
     *            a mask that extracts those parameters of the query (from the entire parameter list) that should be
     *            bound to a fixed value
     * @param parameters
     *            the tuple of fixed values restricting the match set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered match tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(match)) should hold
     * @since 2.0
     */
    public boolean hasMatch(TupleMask parameterSeedMask, ITuple projectedParameterSeed);

    /**
     * Returns the number of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @return the number of pattern matches found.
     */
    public int countMatches(Object[] parameters);

    /**
     * Returns the number of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameterSeedMask
     *            a mask that extracts those parameters of the query (from the entire parameter list) that should be
     *            bound to a fixed value
     * @param parameters
     *            the tuple of fixed values restricting the match set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered match tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(match)) should hold
     * @return the number of pattern matches found.
     * @since 1.7
     */
    public int countMatches(TupleMask parameterSeedMask, ITuple projectedParameterSeed);

    /**
     * Gives an estimate of the number of different groups the matches are projected into by the given mask
     * (e.g. for an identity mask, this means the full match set size). The estimate must meet the required accuracy.
     *
     * <p> If there is insufficient information to provide an answer up to the required precision, {@link Optional#empty()} may be returned.
     * In other words, query backends may deny an answer, or do their best to give an estimate without actually determining the match set of the query.
     * However, caching backends are expected to simply return the indexed (projection) size, initialized on-demand if necessary.
     *
     * <p> PRE: {@link TupleMask#isNonrepeating()} must hold for the group mask.
     *
     * @return if available, an estimate of the cardinality of the projection of the match set, with the desired accuracy.
     *
     * @since 2.1
     */
    public Optional<Long> estimateCardinality(TupleMask groupMask, Accuracy requiredAccuracy);

    /**
     * Gives an estimate of the average size of different groups the matches are projected into by the given mask
     * (e.g. for an identity mask, this means 1, while for an empty mask, the result is match set size).
     * The estimate must meet the required accuracy.
     *
     * <p> If there is insufficient information to provide an answer up to the required precision, {@link Optional#empty()} may be returned.
     * In other words, query backends may deny an answer, or do their best to give an estimate without actually determining the match set of the query.
     * However, caching backends are expected to simply return the exact value from the index, initialized on-demand if necessary.
     *
     * <p> For an empty match set, zero is acceptable as an exact answer.
     *
     * <p> PRE: {@link TupleMask#isNonrepeating()} must hold for the group mask.
     *
     * @return if available, an estimate of the average size of each projection group of the match set, with the desired accuracy.
     *
     * @since 2.1
     */
    public default Optional<Double> estimateAverageBucketSize(TupleMask groupMask, Accuracy requiredAccuracy) {
        return StatisticsHelper.estimateAverageBucketSize(groupMask, requiredAccuracy, this::estimateCardinality);
    }

    /**
     * Returns an arbitrarily chosen match of the pattern that conforms to the given fixed values of some parameters.
     * Neither determinism nor randomness of selection is guaranteed.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @return a match represented in the internal {@link Tuple} representation.
     * @since 2.0
     */
    public Optional<Tuple> getOneArbitraryMatch(Object[] parameters);

    /**
     * Returns an arbitrarily chosen match of the pattern that conforms to the given fixed values of some parameters.
     * Neither determinism nor randomness of selection is guaranteed.
     *
     * @param parameterSeedMask
     *            a mask that extracts those parameters of the query (from the entire parameter list) that should be
     *            bound to a fixed value
     * @param parameters
     *            the tuple of fixed values restricting the match set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered match tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(match)) should hold
     * @return a match represented in the internal {@link Tuple} representation.
     * @since 2.0
     */
    public Optional<Tuple> getOneArbitraryMatch(TupleMask parameterSeedMask, ITuple parameters);

    /**
     * Returns the set of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameters
     *            array where each non-null element binds the corresponding pattern parameter to a fixed value.
     * @pre size of input array must be equal to the number of parameters.
     * @return matches represented in the internal {@link Tuple} representation.
     * @since 2.0
     */
    public Stream<Tuple> getAllMatches(Object[] parameters);

    /**
     * Returns the set of all matches of the pattern that conform to the given fixed values of some parameters.
     *
     * @param parameterSeedMask
     *            a mask that extracts those parameters of the query (from the entire parameter list) that should be
     *            bound to a fixed value
     * @param parameters
     *            the tuple of fixed values restricting the match set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered match tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(match)) should hold
     * @return matches represented in the internal {@link Tuple} representation.
     * @since 2.0
     */
    public Stream<Tuple> getAllMatches(TupleMask parameterSeedMask, ITuple parameters);

    /**
     * The underlying query evaluator backend.
     */
    public IQueryBackend getQueryBackend();

    /**
     * Internal method that registers low-level callbacks for match appearance and disappearance.
     *
     * <p>
     * <b>Caution: </b> This is a low-level callback that is invoked when the pattern matcher is not necessarily in a
     * consistent state yet. Importantly, no model modification permitted during the callback.
     *
     * <p>
     * The callback can be unregistered via invoking {@link #removeUpdateListener(Object)} with the same tag.
     *
     * @param listener
     *            the listener that will be notified of each new match that appears or disappears, starting from now.
     * @param listenerTag
     *            a tag by which to identify the listener for later removal by {@link #removeUpdateListener(Object)}.
     * @param fireNow
     *            if true, the insertion update allback will be immediately invoked on all current matches as a one-time effect.
     *
     * @throws UnsupportedOperationException if this is a non-incremental backend
     * 	(i.e. {@link IQueryBackend#isCaching()} on {@link #getQueryBackend()} returns false)
     */
    public void addUpdateListener(final IUpdateable listener, final Object listenerTag, boolean fireNow);

    /**
     * Removes an existing listener previously registered with the given tag.
     *
     * @throws UnsupportedOperationException if this is a non-incremental backend
     * 	(i.e. {@link IQueryBackend#isCaching()} on {@link #getQueryBackend()} returns false)
     */
    public void removeUpdateListener(final Object listenerTag);

}
