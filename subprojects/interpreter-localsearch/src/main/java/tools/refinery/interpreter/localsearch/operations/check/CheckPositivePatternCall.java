/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.check;

import java.util.List;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.CheckOperationExecutor;
import tools.refinery.interpreter.localsearch.operations.IPatternMatcherOperation;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.util.CallInformation;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.tuple.VolatileModifiableMaskedTuple;

/**
 * @author Grill Balázs
 * @since 1.4
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CheckPositivePatternCall implements ISearchOperation, IPatternMatcherOperation {

    private class Executor extends CheckOperationExecutor {

        private final VolatileModifiableMaskedTuple maskedTuple;
        private IQueryResultProvider matcher;

        public Executor() {
            super();
            this.maskedTuple = new VolatileModifiableMaskedTuple(information.getThinFrameMask());
        }

        @Override
        public void onInitialize(MatchingFrame frame, ISearchContext context) {
            super.onInitialize(frame, context);
            maskedTuple.updateTuple(frame);
            matcher = context.getMatcher(information.getCallWithAdornment());
        }

        /**
         * @since 1.5
         */
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            return matcher.hasMatch(information.getParameterMask(), maskedTuple);
        }

        @Override
        public ISearchOperation getOperation() {
            return CheckPositivePatternCall.this;
        }
    }

    private final CallInformation information;


    /**
     * @since 1.7
     */
    public CheckPositivePatternCall(CallInformation information) {
        super();
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
    public String toString(Function<Integer, String> variableMapping) {
        return "check     find "+information.toString(variableMapping);
    }

    @Override
    public CallInformation getCallInformation() {
        return information;
    }
}
