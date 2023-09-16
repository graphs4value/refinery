/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.extend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
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
 * @since 1.7
 *
 */
public abstract class ExtendBinaryTransitiveClosure implements ISearchOperation, IPatternMatcherOperation {

    private class Executor extends SingleValueExtendOperationExecutor<Object> {

        public Executor(int position) {
            super(position);
        }

        @Override
        public Iterator<?> getIterator(MatchingFrame frame, ISearchContext context) {
            // Note: second parameter is NOT bound during execution, but the first is
            IQueryResultProvider matcher = context.getMatcher(information.getCallWithAdornment());

            Queue<Object> seedsToEvaluate = new LinkedList<>();
            final Object seedValue = frame.get(seedPosition);
            seedsToEvaluate.add(seedValue);
            Set<Object> seedsEvaluated = new HashSet<>();
            Set<Object> targetsFound = new HashSet<>();
            if (reflexive) {
                targetsFound.add(seedValue);
            }

            while(!seedsToEvaluate.isEmpty()) {
                Object currentValue = seedsToEvaluate.poll();
                seedsEvaluated.add(currentValue);
                final Object[] mappedFrame = calculateCallFrame(currentValue);
                matcher.getAllMatches(mappedFrame).forEach(match -> {
                    Object foundTarget = getTarget(match);
                    targetsFound.add(foundTarget);
                    if (!seedsEvaluated.contains(foundTarget)) {
                        seedsToEvaluate.add(foundTarget);
                    }
                });
            }

            return targetsFound.iterator();
        }

        @Override
        public ISearchOperation getOperation() {
            return ExtendBinaryTransitiveClosure.this;
        }
    }

    /**
     * Calculates the transitive closure of a pattern match in a forward direction (first parameter bound, second
     * unbound).
     * </p>
     * <strong>Note</strong>: In case the call is reflexive, it is expected that the bound parameter already matches the universe type of the call.
     *
     * @since 1.7
     */
    public static class Forward extends ExtendBinaryTransitiveClosure {

        private Object[] seedFrame = new Object[2];

        /**
         * @since 2.0
         */
        public Forward(CallInformation information, int sourcePosition, int targetPosition, boolean reflexive) {
            super(information, sourcePosition, targetPosition, reflexive);
        }

        protected Object[] calculateCallFrame(Object seed) {
            seedFrame[0] = seed;
            seedFrame[1] = null;
            return seedFrame;
        }

        protected Object getTarget(Tuple frame) {
            return frame.get(1);
        }
    }

    /**
     * Calculates the transitive closure of a pattern match in a backward direction (first parameter unbound, second
     * bound)
     * </p>
     * <strong>Note</strong>: In case the call is reflexive, it is expected that the bound parameter already matches the universe type of the call.
     *
     * @since 2.0
     */
    public static class Backward extends ExtendBinaryTransitiveClosure {
        private Object[] seedFrame = new Object[2];

        /**
         * @since 2.0
         */
        public Backward(CallInformation information, int sourcePosition, int targetPosition, boolean reflexive) {
            super(information, targetPosition, sourcePosition, reflexive);
        }

        protected Object[] calculateCallFrame(Object seed) {
            seedFrame[0] = null;
            seedFrame[1] = seed;
            return seedFrame;
        }

        protected Object getTarget(Tuple frame) {
            return frame.get(0);
        }
    }

    private final int seedPosition;
    private final int targetPosition;
    private final CallInformation information;
    private final boolean reflexive;

    /**
     * The source position will be matched in the called pattern to the first parameter; while target to the second.
     * @since 2.0
     */
    protected ExtendBinaryTransitiveClosure(CallInformation information, int seedPosition, int targetPosition, boolean reflexive) {
        this.information = information;
        this.seedPosition = seedPosition;
        this.targetPosition = targetPosition;
        this.reflexive = reflexive;
    }

    protected abstract Object[] calculateCallFrame(Object seed);

    protected abstract Object getTarget(Tuple frame);

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor(targetPosition);
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(Function<Integer, String> variableMapping) {
        String c = information.toString(variableMapping);
        int p = c.indexOf('(');
        return "extend    find " + c.substring(0, p) + "+" + c.substring(p);
    }

    @Override
    public List<Integer> getVariablePositions() {
        return Arrays.asList(seedPosition, targetPosition);
    }

    @Override
    public CallInformation getCallInformation() {
        return information;
    }
}
