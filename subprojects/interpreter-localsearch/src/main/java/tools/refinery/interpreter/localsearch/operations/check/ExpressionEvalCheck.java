/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQueryLabs
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
import java.util.Set;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.CheckOperationExecutor;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.MatchingFrameValueProvider;
import tools.refinery.interpreter.matchers.psystem.IExpressionEvaluator;

/**
 * @author Grill Balázs
 * @since 1.3
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ExpressionEvalCheck implements ISearchOperation {

    private class Executor extends CheckOperationExecutor {

        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            try {
                Object result = evaluator.evaluateExpression(new MatchingFrameValueProvider(frame, nameMap));
                if (!unwind && result != null) {
                    Object currentValue = frame.get(outputPosition);
                    return result.equals(currentValue);
                } else if (unwind && result instanceof Set<?>) {
                    Object currentValue = frame.get(outputPosition);
                    return ((Set<?>)result).contains(currentValue);
                }
            } catch (Exception e) {
                context.getLogger().warn("Error while evaluating expression", e);
            }
            return false;
        }

        @Override
        public ISearchOperation getOperation() {
            return ExpressionEvalCheck.this;
        }
    }

    private final int outputPosition;
    private final IExpressionEvaluator evaluator;
    private final Map<String, Integer> nameMap;
    private final boolean unwind;

    public ExpressionEvalCheck(IExpressionEvaluator evaluator, Map<String, Integer> nameMap, int position) {
        this(evaluator, nameMap, false, position);
    }

    /**
     * @since 2.7
     */
    public ExpressionEvalCheck(IExpressionEvaluator evaluator, Map<String, Integer> nameMap, boolean unwind, int position) {
        this.evaluator = evaluator;
        this.nameMap = nameMap;
        this.unwind = unwind;
        this.outputPosition = position;
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
        return "check     "+variableMapping.apply(outputPosition)+" = expression "+evaluator.getShortDescription();
    }
}
