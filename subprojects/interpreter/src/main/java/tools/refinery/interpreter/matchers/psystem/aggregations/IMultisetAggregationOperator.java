/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.aggregations;

import java.util.Collection;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.aggregators.ExtremumOperator;

/**
 * A single column aggregator is used to incrementally compute the aggregate of a multiset of values according to an aggregator operator.
 *
 * <p> The operator provides two methods of computation: <ul>
 *  <li>Stateless aggregation of an explicit multiset, provided by {@link #aggregateStatelessly(Collection)}.</li>
 *  <li>Incremental aggregation, provided by {@link #createNeutral()}, {@link #update(Object, Object, boolean)}, {@link #isNeutral(Object)}, {@link #getAggregate(Object)}.
 * </ul>
 *
 * <p> In case of incremental computation, the aggregable multiset is conceptual; it is not represented by an explicit Collection<Domain> object, but its update operations are tracked.
 *
 * <p> In case of incremental computation, internal results, potentially distinct from the final aggregate result, may be stored in a helper data structure called <b>accumulator</b>.
 * The goal of this distinction is that the final result may not be sufficient for incremental updates (see e.g. {@link ExtremumOperator}).
 *
 * @author Gabor Bergmann
 *
 * @param <Domain> the type of elements to be aggregated.
 * @param <Accumulator> the type used to store the interim results of the aggregate computation,
 *  that may be incrementally refreshed upon updates to the multiset, and that can easily yield the final result.
 * @param <AggregateResult> the type of the final result of the aggregation to be output.
 *
 * @since 1.4
 */
public interface IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> {

    /**
     * A textual description of the operator.
     */
    String getShortDescription();

    /**
     * A name or identifier of the operator.
     */
    String getName();

    /**
     * @return the neutral element, i.e. the interim result of aggregating an empty multiset.
     */
    Accumulator createNeutral();

    /**
     * @return true if the interim result is equivalent to the neutral element, as if there are no values in the multiset.
     * Must return true if the multiset is empty.
     */
    boolean isNeutral(Accumulator result);

    /**
     * @return an updated intermediate result,
     *  changed to reflect that a given object was added to / removed from the multiset
     *      (as indicated by the parameter isInsertion)
     */
    Accumulator update(Accumulator oldResult, Domain updateValue, boolean isInsertion);

    /**
     * @return the aggregate result obtained from the given intermediate result.
     * May be null to indicate that the current multiset cannot be aggregated (e.g. 0 elements have no minimum).
     */
    AggregateResult getAggregate(Accumulator result);

    /**
     * Calculates the aggregate results from a given stream of values; all values are considered as inserted
     * @return the aggregate result, or null if no result can be calculated (e.g. because of an empty stream)
     * @since 2.0
     */
    AggregateResult aggregateStream(Stream<Domain> stream);

    /**
     * Clones the given accumulator (with all its internal contents).
     */
    default Accumulator clone(Accumulator original) {
        throw new UnsupportedOperationException();
    }

    /**
     * Combines the given aggregate result and accumulator into a single aggregate result.
     */
    default AggregateResult combine(AggregateResult left, Accumulator right) {
        throw new UnsupportedOperationException();
    }

    default boolean contains(Domain value, Accumulator accumulator) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pretty prints the contents of the given accumulator.
     */
    default String prettyPrint(final Accumulator accumulator) {
        return accumulator.toString();
    }

}
