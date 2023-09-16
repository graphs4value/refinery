/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.extend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.MatchingFrameValueProvider;
import tools.refinery.interpreter.matchers.psystem.IExpressionEvaluator;

/**
 * Calculates the result of an expression and stores it inside a variable for future reference.
 *
 * @author Zoltan Ujhelyi
 *
 */
public class ExpressionEval implements ISearchOperation {

    private class Executor extends SingleValueExtendOperationExecutor<Object> {

        public Executor(int position) {
            super(position);
        }

        @Override
        public Iterator<?> getIterator(MatchingFrame frame, ISearchContext context) {
            try {
                Object result = evaluator.evaluateExpression(new MatchingFrameValueProvider(frame, nameMap));
                if (!unwind && result != null){
                    return Collections.singletonList(result).iterator();
                } else if (unwind && result instanceof Set<?>) {
                    return ((Set<?>)result).iterator();
                } else {
                    return Collections.emptyIterator();
                }
            } catch (Exception e) {
                context.getLogger().warn("Error while evaluating expression", e);
                return Collections.emptyIterator();
            }
        }

        @Override
        public ISearchOperation getOperation() {
            return ExpressionEval.this;
        }
    }

    private final IExpressionEvaluator evaluator;
    private final boolean unwind;
    private final Map<String, Integer> nameMap;
    private final int position;

    public ExpressionEval(IExpressionEvaluator evaluator, Map<String, Integer> nameMap, int position) {
        this(evaluator, nameMap, false, position);
    }

    /**
     * @since 2.7
     */
    public ExpressionEval(IExpressionEvaluator evaluator, Map<String, Integer> nameMap, boolean unwind, int position) {
        this.evaluator = evaluator;
        this.nameMap = nameMap;
        this.unwind = unwind;
        this.position = position;
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    -"+variableMapping.apply(position)+" = expression "+evaluator.getShortDescription();
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(position);
    }

    @Override
    public List<Integer> getVariablePositions() {
        // XXX not sure if this is the correct implementation to get the affected variable indicies
        List<Integer> variables = new ArrayList<>();
        variables.addAll(nameMap.values());
        return variables;
    }

}
