/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.store.query.viatra.internal.localsearch;

import tools.refinery.viatra.runtime.localsearch.MatchingFrame;
import tools.refinery.viatra.runtime.localsearch.matcher.ISearchContext;
import tools.refinery.viatra.runtime.localsearch.operations.IPatternMatcherOperation;
import tools.refinery.viatra.runtime.localsearch.operations.ISearchOperation;
import tools.refinery.viatra.runtime.localsearch.operations.util.CallInformation;
import tools.refinery.viatra.runtime.matchers.backend.IQueryResultProvider;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.VolatileModifiableMaskedTuple;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * @author Grill Balázs
 * @since 1.4
 *
 */
public class ExtendPositivePatternCall implements ISearchOperation, IPatternMatcherOperation {

    private class Executor extends ExtendOperationExecutor<Tuple> {
        private final VolatileModifiableMaskedTuple maskedTuple;

        public Executor() {
            maskedTuple = new VolatileModifiableMaskedTuple(information.getThinFrameMask());
        }

        @Override
        protected Iterator<? extends Tuple> getIterator(MatchingFrame frame, ISearchContext context) {
            maskedTuple.updateTuple(frame);
            IQueryResultProvider matcher = context.getMatcher(information.getCallWithAdornment());
            return matcher.getAllMatches(information.getParameterMask(), maskedTuple).iterator();
        }

        /**
         * @since 2.0
         */
        @Override
        protected boolean fillInValue(Tuple result, MatchingFrame frame, ISearchContext context) {
            TupleMask mask = information.getFullFrameMask();
            // The first loop clears out the elements from a possible previous iteration
            for(int i : information.getFreeParameterIndices()) {
                mask.set(frame, i, null);
            }
            for(int i : information.getFreeParameterIndices()) {
                Object oldValue = mask.getValue(frame, i);
                Object valueToFill = result.get(i);
                if (oldValue != null && !oldValue.equals(valueToFill)){
                    // If the inverse map contains more than one values for the same key, it means that these arguments are unified by the caller.
                    // In this case if the callee assigns different values the frame shall be dropped
                    return false;
                }
                mask.set(frame, i, valueToFill);
            }
            return true;
        }

        @Override
        protected void cleanup(MatchingFrame frame, ISearchContext context) {
            TupleMask mask = information.getFullFrameMask();
            for(int i : information.getFreeParameterIndices()){
                mask.set(frame, i, null);
            }

        }

        @Override
        public ISearchOperation getOperation() {
            return ExtendPositivePatternCall.this;
        }
    }

    private final CallInformation information;

    /**
     * @since 1.7
     */
    public ExtendPositivePatternCall(CallInformation information) {
       this.information = information;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }

    @Override
    public List<Integer> getVariablePositions() {
        return information.getVariablePositions();
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(@SuppressWarnings("squid:S4276") Function<Integer, String> variableMapping) {
        return "extend find " + information.toString(variableMapping);
    }

    @Override
    public CallInformation getCallInformation() {
        return information;
    }
}
