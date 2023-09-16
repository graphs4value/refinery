/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.check;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.CheckOperationExecutor;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;

/**
 * This operation handles constants in search plans by checking if a variable is bound to a certain constant value. Such
 * operations should be executed as early as possible during plan execution.
 *
 * @author Marton Bur
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CheckConstant implements ISearchOperation {

    private class Executor extends CheckOperationExecutor {

        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            return frame.get(position).equals(value);
        }

        @Override
        public ISearchOperation getOperation() {
            return CheckConstant.this;
        }
    }

    private int position;
    private Object value;

    public CheckConstant(int position, Object value) {
        this.position = position;
        this.value = value;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Collections.singletonList(position);
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "check     constant "+variableMapping.apply(position)+"='"+value+"'";
    }

}
