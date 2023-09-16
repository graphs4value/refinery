/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.localsearch.operations.IIteratingSearchOperation;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;

/**
 * @author Grill Balázs
 * @since 1.4
 *
 */
public class PlanDescriptor implements IPlanDescriptor {

    private final PQuery pquery;
    private final List<SearchPlanForBody> plan;
    private final Set<PParameter> adornment;
    private Set<IInputKey> iteratedKeys = null;

    public PlanDescriptor(PQuery pquery, Collection<SearchPlanForBody> plan, Set<PParameter> adornment) {
        this.pquery = pquery;
        this.plan = new ArrayList<>(plan);
        this.adornment = adornment;
    }

    @Override
    public PQuery getQuery() {
        return pquery;
    }

    @Override
    public Collection<SearchPlanForBody> getPlan() {
        return plan;
    }

    @Override
    public Set<PParameter> getAdornment() {
        return adornment;
    }

    @Override
    public Set<IInputKey> getIteratedKeys() {
        if (iteratedKeys == null){
            Set<IInputKey> keys = new HashSet<>();
            for(SearchPlanForBody bodyPlan : plan){
                for(ISearchOperation operation : bodyPlan.getCompiledOperations()){
                    if (operation instanceof IIteratingSearchOperation){
                        keys.add(((IIteratingSearchOperation) operation).getIteratedInputKey());
                    }
                }
            }
            iteratedKeys = Collections.unmodifiableSet(keys);
        }
        return iteratedKeys;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Plan for ").append(pquery.getFullyQualifiedName()).append("(")
        .append(adornment.stream().map(PParameter::getName).collect(Collectors.joining(",")))
        .append("{")
        .append(plan.stream().map(Object::toString).collect(Collectors.joining("}\n{")))
        .append("}")
        .toString();
    }

}
