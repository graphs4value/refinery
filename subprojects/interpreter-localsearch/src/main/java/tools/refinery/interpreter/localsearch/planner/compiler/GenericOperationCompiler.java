/*******************************************************************************
 * Copyright (c) 2010-2017, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner.compiler;

import tools.refinery.interpreter.localsearch.operations.generic.GenericTypeCheck;
import tools.refinery.interpreter.localsearch.operations.generic.GenericTypeExtend;
import tools.refinery.interpreter.localsearch.operations.generic.GenericTypeExtendSingleValue;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.TypeFilterConstraint;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.TypeConstraint;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleMask;

import java.util.*;

/**
 * @author Zoltan Ujhelyi
 * @since 1.7
 *
 */
public class GenericOperationCompiler extends AbstractOperationCompiler {

    public GenericOperationCompiler(IQueryRuntimeContext runtimeContext) {
        super(runtimeContext);
    }

    @Override
    protected void createCheck(TypeFilterConstraint typeConstraint, Map<PVariable, Integer> variableMapping) {
        IInputKey inputKey = typeConstraint.getInputKey();
        Tuple tuple = typeConstraint.getVariablesTuple();
        int[] positions = new int[tuple.getSize()];
        for (int i = 0; i < tuple.getSize(); i++) {
            PVariable variable = (PVariable) tuple.get(i);
            positions[i] = variableMapping.get(variable);
        }
        operations.add(new GenericTypeCheck(inputKey, positions, TupleMask.fromSelectedIndices(variableMapping.size(), positions)));

    }

    @Override
    protected void createCheck(TypeConstraint typeConstraint, Map<PVariable, Integer> variableMapping) {
        IInputKey inputKey = typeConstraint.getSupplierKey();
        Tuple tuple = typeConstraint.getVariablesTuple();
        int[] positions = new int[tuple.getSize()];
        for (int i = 0; i < tuple.getSize(); i++) {
            PVariable variable = (PVariable) tuple.get(i);
            positions[i] = variableMapping.get(variable);
        }
        operations.add(new GenericTypeCheck(inputKey, positions, TupleMask.fromSelectedIndices(variableMapping.size(), positions)));
    }

    @Override
    protected void createUnaryTypeCheck(IInputKey inputKey, int position) {
        int[] positions = new int[] {position};
        operations.add(new GenericTypeCheck(inputKey, positions, TupleMask.fromSelectedIndices(1, positions)));
    }

    @Override
    protected void createExtend(TypeConstraint typeConstraint, Map<PVariable, Integer> variableMapping) {
        IInputKey inputKey = typeConstraint.getSupplierKey();
        Tuple tuple = typeConstraint.getVariablesTuple();

        int[] positions = new int[tuple.getSize()];
        List<Integer> boundVariableIndices = new ArrayList<>();
        List<Integer> boundVariables = new ArrayList<>();
        Set<Integer> unboundVariables = new HashSet<>();
        for (int i = 0; i < tuple.getSize(); i++) {
            PVariable variable = (PVariable) tuple.get(i);
            Integer position = variableMapping.get(variable);
            positions[i] = position;
            if (variableBindings.get(typeConstraint).contains(position)) {
                boundVariableIndices.add(i);
                boundVariables.add(position);
            } else {
                unboundVariables.add(position);
            }
        }
        TupleMask indexerMask = TupleMask.fromSelectedIndices(inputKey.getArity(), boundVariableIndices);
        TupleMask callMask = TupleMask.fromSelectedIndices(variableMapping.size(), boundVariables);
		// If multiple tuple elements from the indexer should be bound to the same variable, we must use a
		// {@link GenericTypeExtend} check whether the tuple elements have the same value.
		if (unboundVariables.size() == 1 && indexerMask.getSize() + 1 == indexerMask.getSourceWidth()) {
            operations.add(new GenericTypeExtendSingleValue(inputKey, positions, callMask, indexerMask, unboundVariables.iterator().next()));
        } else {
            operations.add(new GenericTypeExtend(inputKey, positions, callMask, indexerMask, unboundVariables));
        }

    }



}
