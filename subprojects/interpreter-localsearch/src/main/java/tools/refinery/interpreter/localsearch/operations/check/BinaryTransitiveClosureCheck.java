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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.CheckOperationExecutor;
import tools.refinery.interpreter.localsearch.operations.IPatternMatcherOperation;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.operations.util.CallInformation;
import tools.refinery.interpreter.matchers.backend.IQueryResultProvider;
import tools.refinery.interpreter.matchers.tuple.Tuple;

/**
 * Checking for a transitive closure expressed as a local search pattern matcher. The matched pattern must have two
 * parameters of the same model type.
 *
 * @author Zoltan Ujhelyi
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *
 */
public class BinaryTransitiveClosureCheck implements ISearchOperation, IPatternMatcherOperation {

    private class Executor extends CheckOperationExecutor {

        @Override
        public void onInitialize(MatchingFrame frame, ISearchContext context) {
            super.onInitialize(frame, context);
            matcher = context.getMatcher(information.getCallWithAdornment());
            // Note: second parameter is NOT bound during execution, but the first is
        }

        @Override
        protected boolean check(MatchingFrame frame, ISearchContext context) {
            if (checkReflexive(frame)) {
                return true;
            }

            Object targetValue = frame.get(targetPosition);
            Queue<Object> sourcesToEvaluate = new LinkedList<>();
            sourcesToEvaluate.add(frame.get(sourcePosition));
            Set<Object> sourceEvaluated = new HashSet<>();
            final Object[] mappedFrame = new Object[] {null, null};
            while (!sourcesToEvaluate.isEmpty()) {
                Object currentValue = sourcesToEvaluate.poll();
                sourceEvaluated.add(currentValue);
                mappedFrame[0] = currentValue;
                for (Tuple match : (Iterable<Tuple>) () -> matcher.getAllMatches(mappedFrame).iterator()) {
                    Object foundTarget = match.get(1);
                    if (targetValue.equals(foundTarget)) {
                        return true;
                    } else if (!sourceEvaluated.contains(foundTarget)) {
                        sourcesToEvaluate.add(foundTarget);
                    }
                }
            }
            return false;
        }

        protected boolean checkReflexive(MatchingFrame frame) {
            return reflexive && Objects.equals(frame.get(sourcePosition), frame.get(targetPosition));
        }

        @Override
        public ISearchOperation getOperation() {
            return BinaryTransitiveClosureCheck.this;
        }
    }

    private final CallInformation information;
    private IQueryResultProvider matcher;
    private final int sourcePosition;
    private final int targetPosition;
    private final boolean reflexive;

    /**
     * The source position will be matched in the called pattern to the first parameter; while target to the second.
     * </p>
     * <strong>NOTE</strong>: the reflexive check call does not include the parameter type checks; appropriate type checks should be
     * added as necessary by the operation compiler.
     *
     * @since 2.0
     */
    public BinaryTransitiveClosureCheck(CallInformation information, int sourcePosition, int targetPosition, boolean reflexive) {
        super();
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.information = information;
        this.reflexive = reflexive;
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
        String c = information.toString(variableMapping);
        int p = c.indexOf('(');
        String modifier = reflexive ? "*" : "+";
        return "check     find "+c.substring(0, p)+ modifier +c.substring(p);
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(sourcePosition, targetPosition);
    }

    @Override
    public CallInformation getCallInformation() {
        return information;
    }
}
