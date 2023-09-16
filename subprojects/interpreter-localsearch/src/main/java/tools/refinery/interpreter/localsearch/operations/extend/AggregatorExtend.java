/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.extend;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.IPatternMatcherOperation;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.util.CallInformation;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.psystem.aggregations.IMultisetAggregationOperator;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.AggregatorConstraint;
import tools.refinery.interpreter.matchers.tuple.VolatileModifiableMaskedTuple;

/**
 * Calculates the aggregated value of a column based on the given {@link AggregatorConstraint}
 *
 * @author Balázs Grill
 * @since 1.4
 */
public class AggregatorExtend  implements ISearchOperation, IPatternMatcherOperation{

    private class Executor extends SingleValueExtendOperationExecutor<Object> {

        private final VolatileModifiableMaskedTuple maskedTuple;
        private IQueryResultProvider matcher;

        public Executor(int position) {
            super(position);
            this.maskedTuple = new VolatileModifiableMaskedTuple(information.getThinFrameMask());
        }

        @Override
        public Iterator<?> getIterator(MatchingFrame frame, ISearchContext context) {
            maskedTuple.updateTuple(frame);
            matcher = context.getMatcher(information.getCallWithAdornment());
            Object aggregate = aggregate(aggregator.getAggregator().getOperator(), aggregator.getAggregatedColumn());
            return aggregate == null ? Collections.emptyIterator() : Collections.singletonList(aggregate).iterator();

        }

        @SuppressWarnings("unchecked")
        private <Domain, Accumulator, AggregateResult> AggregateResult aggregate(
                IMultisetAggregationOperator<Domain, Accumulator, AggregateResult> operator, int aggregatedColumn) {
            final Stream<Domain> valueStream = matcher.getAllMatches(information.getParameterMask(), maskedTuple)
                    .map(match -> (Domain) match.get(aggregatedColumn));
            return operator.aggregateStream(valueStream);
        }

        @Override
        public ISearchOperation getOperation() {
            return AggregatorExtend.this;
        }
    }

    private final AggregatorConstraint aggregator;
    private final CallInformation information;
    private final int position;

    /**
     * @since 1.7
     */
    public AggregatorExtend(CallInformation information, AggregatorConstraint aggregator, int position) {
        this.aggregator = aggregator;
        this.information = information;
        this.position = position;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(position);
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(position);
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    -"+variableMapping.apply(position)+" = " + aggregator.getAggregator().getOperator().getName()+" find " + information.toString(variableMapping);
    }

    @Override
    public CallInformation getCallInformation() {
        return information;
    }
}
