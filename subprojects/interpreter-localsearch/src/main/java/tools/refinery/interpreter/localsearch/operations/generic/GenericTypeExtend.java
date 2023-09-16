/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.localsearch.matcher.ISearchContext;
import tools.refinery.interpreter.localsearch.operations.ExtendOperationExecutor;
import tools.refinery.interpreter.localsearch.operations.IIteratingSearchOperation;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.tuple.VolatileMaskedTuple;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 * @noextend This class is not intended to be subclassed by clients.
 */
public class GenericTypeExtend implements IIteratingSearchOperation {

    private class Executor extends ExtendOperationExecutor<Tuple> {
        private final VolatileMaskedTuple maskedTuple;

        public Executor() {
            this.maskedTuple = new VolatileMaskedTuple(callMask);
        }

        @Override
        protected Iterator<? extends Tuple> getIterator(MatchingFrame frame, ISearchContext context) {
            maskedTuple.updateTuple(frame);
            return context.getRuntimeContext().enumerateTuples(type, indexerMask, maskedTuple).iterator();
        }

        @Override
        protected boolean fillInValue(Tuple newTuple, MatchingFrame frame, ISearchContext context) {
            for (Integer position : unboundVariableIndices) {
                frame.setValue(position, null);
            }
            for (int i = 0; i < positions.length; i++) {
                Object newValue = newTuple.get(i);
                Object oldValue = frame.getValue(positions[i]);
                if (oldValue != null && !Objects.equals(oldValue, newValue)) {
                    // If positions tuple maps more than one values for the same element (e.g. loop), it means that
                    // these arguments are to unified by the caller. In this case if the callee assigns different values
                    // the frame shall be considered a failed match
                    return false;
                }
                frame.setValue(positions[i], newValue);
            }
            return true;
        }

        @Override
        protected void cleanup(MatchingFrame frame, ISearchContext context) {
            for (Integer position : unboundVariableIndices) {
                frame.setValue(position, null);
            }
        }

        @Override
        public ISearchOperation getOperation() {
            return GenericTypeExtend.this;
        }
    }

    private final IInputKey type;
    private final int[] positions;
    private final List<Integer> positionList;
    private final Set<Integer> unboundVariableIndices;
    private final TupleMask indexerMask;
    private final TupleMask callMask;

    /**
     *
     * @param type
     *            the type to execute the extend operation on
     * @param positions
     *            the parameter positions that represent the variables of the input key
     * @param unboundVariableIndices
     *            the set of positions that are bound at the start of the operation
     */
    public GenericTypeExtend(IInputKey type, int[] positions, TupleMask callMask, TupleMask indexerMask, Set<Integer> unboundVariableIndices) {
        Preconditions.checkArgument(positions.length == type.getArity(),
                "The type %s requires %d parameters, but %d positions are provided", type.getPrettyPrintableName(),
                type.getArity(), positions.length);
        List<Integer> modifiablePositionList = new ArrayList<>();
        for (int position : positions) {
            modifiablePositionList.add(position);
        }
        this.positionList = Collections.unmodifiableList(modifiablePositionList);
        this.positions = positions;
        this.type = type;

        this.unboundVariableIndices = unboundVariableIndices;
        this.indexerMask = indexerMask;
        this.callMask = callMask;
    }

    @Override
    public IInputKey getIteratedInputKey() {
        return type;
    }

    @Override
    public ISearchOperationExecutor createExecutor() {
        return new Executor();
    }

    @Override
    public List<Integer> getVariablePositions() {
        return positionList;
    }

    @Override
    public String toString() {
        return toString(Object::toString);
    }

    @Override
    public String toString(Function<Integer, String> variableMapping) {
        return "extend    " + type.getPrettyPrintableName() + "("
                + positionList.stream()
                        .map(input -> String.format("%s%s", unboundVariableIndices.contains(input) ? "-" : "+", variableMapping.apply(input)))
                        .collect(Collectors.joining(", "))
                + ")";
    }

}
