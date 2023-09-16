/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.aggregators;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;

/**
 * Incrementally computes the minimum or maximum of java.lang.Comparable values, using the default comparison
 *
 * @author Gabor Bergmann
 * @since 1.4
 */
public class ExtremumOperator<T extends Comparable<T>>
        implements IMultisetAggregationOperator<T, SortedMap<T, Integer>, T> {

    public enum Extreme {
        MIN, MAX;

        /**
         * @since 2.0
         */
        public <T> T pickFrom(SortedMap<T, Integer> nonEmptyMultiSet) {
            switch(this) {
            case MIN:
                return nonEmptyMultiSet.firstKey();
            case MAX:
                return nonEmptyMultiSet.lastKey();
            default:
                return null;
            }
        }
    }

    private static final ExtremumOperator MIN_OP = new ExtremumOperator<>(Extreme.MIN);
    private static final ExtremumOperator MAX_OP = new ExtremumOperator<>(Extreme.MAX);

    public static <T extends Comparable<T>> ExtremumOperator<T> getMin() {
        return MIN_OP;
    }
    public static <T extends Comparable<T>> ExtremumOperator<T> getMax() {
        return MAX_OP;
    }

    Extreme extreme;
    private ExtremumOperator(Extreme extreme) {
        super();
        this.extreme = extreme;
    }

    @Override
    public String getShortDescription() {
        String opName = getName();
        return String.format(
                "%s incrementally computes the %simum of java.lang.Comparable values, using the default comparison",
                opName, opName);
    }

    @Override
    public String getName() {
        return extreme.name().toLowerCase();
    }

    /**
     * @since 2.0
     */
    @Override
    public SortedMap<T, Integer> createNeutral() {
        return new TreeMap<>();
    }

    /**
     * @since 2.0
     */
    @Override
    public boolean isNeutral(SortedMap<T, Integer> result) {
        return result.isEmpty();
    }

    /**
     * @since 2.0
     */
    @Override
    public SortedMap<T, Integer> update(SortedMap<T, Integer> oldResult, T updateValue, boolean isInsertion) {
        oldResult.compute(updateValue, (value, c) -> {
            int count = (c == null) ? 0 : c;
            int result = (isInsertion) ? count+1 : count-1;
            return (result == 0) ? null : result;
        });
        return oldResult;
    }

    /**
     * @since 2.0
     */
    @Override
    public T getAggregate(SortedMap<T, Integer> result) {
        return result.isEmpty() ? null :
            extreme.pickFrom(result);
    }

    /**
     * @since 2.0
     */
    @Override
    public T aggregateStream(Stream<T> stream) {
        switch (extreme) {
        case MIN:
            return stream.min(Comparator.naturalOrder()).orElse(null);
        case MAX:
            return stream.max(Comparator.naturalOrder()).orElse(null);
        default:
            return null;
        }
    }

    /**
     * @since 2.4
     */
    @Override
    public SortedMap<T, Integer> clone(SortedMap<T, Integer> original) {
        return new TreeMap<T, Integer>(original);
    }

}
