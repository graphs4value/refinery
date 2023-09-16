/*******************************************************************************
 * Copyright (c) 2010-2016, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tools.refinery.interpreter.localsearch.matcher.CallWithAdornment;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.tuple.TupleMask;

/**
 * This class is responsible for storing the results of the planner and operation compiler for a selected body.
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class SearchPlanForBody {

    private final PBody body;
    private final Map<PVariable, Integer> variableKeys;
    private final int[] parameterKeys;
    private final SubPlan plan;
    private final List<ISearchOperation> compiledOperations;
    private final Collection<CallWithAdornment> dependencies;
    private final double cost;
    private final Object internalRepresentation;

    /**
     * @since 2.1
     */
    public SearchPlanForBody(PBody body, Map<PVariable, Integer> variableKeys,
            SubPlan plan, List<ISearchOperation> compiledOperations, Collection<CallWithAdornment> dependencies,
            Object internalRepresentation, double cost) {
        super();
        this.body = body;
        this.variableKeys = variableKeys;
        this.plan = plan;
        this.internalRepresentation = internalRepresentation;
        this.cost = cost;
        List<PVariable> parameters = body.getSymbolicParameterVariables();
        parameterKeys = new int[parameters.size()];
        for (int i=0; i<parameters.size(); i++) {
            parameterKeys[i] = variableKeys.get(parameters.get(i));
        }
        this.compiledOperations = new ArrayList<>(compiledOperations.size()+1);
        this.compiledOperations.addAll(compiledOperations);

        this.dependencies = new ArrayList<>(dependencies);
    }

    public PBody getBody() {
        return body;
    }

    public Map<PVariable, Integer> getVariableKeys() {
        return variableKeys;
    }

    public int[] getParameterKeys() {
        return Arrays.copyOf(parameterKeys, parameterKeys.length);
    }

    public List<ISearchOperation> getCompiledOperations() {
        return compiledOperations;
    }

    public SubPlan getPlan() {
        return plan;
    }

    public Collection<CallWithAdornment> getDependencies() {
        return dependencies;
    }

    public TupleMask calculateParameterMask() {
        return TupleMask.fromSelectedIndices(variableKeys.size(), parameterKeys);
    }

    @Override
    public String toString() {
        return compiledOperations.stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    /**
     * @since 2.1
     */
    public double getCost() {
        return cost;
    }

    /**
     * @return The internal representation of the search plan, if any, for traceability
     * @since 2.1
     */
    public Object getInternalRepresentation() {
        return internalRepresentation;
    }




}
