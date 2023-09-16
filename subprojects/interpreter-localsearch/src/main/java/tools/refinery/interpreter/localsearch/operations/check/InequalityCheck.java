/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.check;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.exceptions.LocalSearchException;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.CheckOperationExecutor;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;

/**
 * @author Zoltan Ujhelyi
 * @noextend This class is not intended to be subclassed by clients.
 */
public class InequalityCheck implements ISearchOperation {

    private class Executor extends CheckOperationExecutor {

        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            Object source = frame.getValue(sourceLocation);
            Object target = frame.getValue(targetLocation);
            if (source == null) {
                throw new LocalSearchException("Source not bound.");
            }
            if (target == null) {
                throw new LocalSearchException("Target not bound");
            }
            return !source.equals(target);
        }

        @Override
        public ISearchOperation getOperation() {
            return InequalityCheck.this;
        }
    }

    int sourceLocation;
    int targetLocation;

    public InequalityCheck(int sourceLocation, int targetLocation) {
        super();
        this.sourceLocation = sourceLocation;
        this.targetLocation = targetLocation;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "check     "+variableMapping.apply(sourceLocation)+" != "+variableMapping.apply(targetLocation);
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(sourceLocation, targetLocation);
    }

}
