/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.CheckOperationExecutor;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.MatchingFrameValueProvider;
import tools.refinery.interpreter.matchers.psystem.IExpressionEvaluator;

/**
 * @author Zoltan Ujhelyi
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ExpressionCheck implements ISearchOperation {

    private class Executor extends CheckOperationExecutor {

        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            try {
                boolean result = (Boolean) evaluator.evaluateExpression(new MatchingFrameValueProvider(frame, nameMap));
                return result;
            } catch (Exception e) {
                context.getLogger().warn("Error while evaluating expression", e);
                return false;
            }
        }

        @Override
        public ISearchOperation getOperation() {
            return ExpressionCheck.this;
        }
    }

    IExpressionEvaluator evaluator;
    Map<String, Integer> nameMap;

    public ExpressionCheck(IExpressionEvaluator evaluator, Map<String, Integer> nameMap) {
        super();
        this.evaluator = evaluator;
        this.nameMap = nameMap;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }

    @Override
    public List<Integer> getVariablePositions() {
        // XXX not sure if this is the correct implementation to get the affected variable indicies
        return new ArrayList<>(nameMap.values());
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "check     expression "+evaluator.getShortDescription();
    }

}
