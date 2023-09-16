/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.tuple.TupleMask;

/**
 * A SearchPlan stores a collection of SearchPlanOperations for a fixed order of variables.
 *
 * @author Zoltan Ujhelyi
 *
 */
public class SearchPlan {

    private final List<ISearchOperation> operations;
    private final Map<Integer, PVariable> variableMapping;
    private final TupleMask parameterMask;
    private final PBody body;

    /**
     * @since 2.0
     */
    public SearchPlan(PBody body, List<ISearchOperation> operations, TupleMask parameterMask, Map<PVariable, Integer> variableMapping) {
        this.body = body;
        this.operations = Collections.unmodifiableList(new ArrayList<>(operations));
        this.parameterMask = parameterMask;
        this.variableMapping = Collections.unmodifiableMap(variableMapping.entrySet().stream()
                .collect(Collectors.toMap(Entry::getValue, Entry::getKey)));
    }


    /**
     * Returns an immutable list of operations stored in the plan.
     * @return the operations
     */
    public List<ISearchOperation> getOperations() {
        return operations;
    }

    /**
     * Returns an immutable map of variable mappings for the plan
     * @since 2.0
     */
    public Map<Integer, PVariable> getVariableMapping() {
        return variableMapping;
    }

    /**
     * Returns the index of a given operation in the plan
     * @since 2.0
     */
    public int getOperationIndex(ISearchOperation operation) {
        return operations.indexOf(operation);
    }

    /**
     * @since 2.0
     */
    public TupleMask getParameterMask() {
        return parameterMask;
    }

    /**
     * @since 2.0
     */
    public PBody getSourceBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{\n");
        for(ISearchOperation operation : this.getOperations()){
            sb.append("\t");
            sb.append(operation);
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
