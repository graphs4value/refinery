/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.extend;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.IPatternMatcherOperation;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.util.CallInformation;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.tuple.VolatileModifiableMaskedTuple;

/**
 * Calculates the count of matches for a called matcher
 *
 * @author Zoltan Ujhelyi
 */
public class CountOperation implements ISearchOperation, IPatternMatcherOperation{

    private class Executor extends SingleValueExtendOperationExecutor<Integer>  {
        private final VolatileModifiableMaskedTuple maskedTuple;
        private IQueryResultProvider matcher;

        public Executor(int position) {
            super(position);
            maskedTuple = new VolatileModifiableMaskedTuple(information.getThinFrameMask());
        }

        @Override
        public Iterator<Integer> getIterator(MatchingFrame frame, ISearchContext context) {
            matcher = context.getMatcher(information.getCallWithAdornment());
            maskedTuple.updateTuple(frame);
            return Collections.singletonList(matcher.countMatches(information.getParameterMask(), maskedTuple)).iterator();
        }

        @Override
        public ISearchOperation getOperation() {
            return CountOperation.this;
        }
    }

    private final CallInformation information;
    private final int position;

    /**
     * @since 1.7
     */
    public CountOperation(CallInformation information, int position) {
        this.information = information;
        this.position = position;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(position);
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
        return "extend    -"+variableMapping.apply(position)+" = count find " + information.toString(variableMapping);
    }

    @Override
    public CallInformation getCallInformation() {
        return information;
    }
}
