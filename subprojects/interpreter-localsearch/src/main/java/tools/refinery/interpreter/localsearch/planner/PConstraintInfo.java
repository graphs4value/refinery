/**
 * Copyright (c) 2010-2015, Marton Bur, Zoltan Ujhelyi, Akos Horvath, Istvan Rath and Danil Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.localsearch.planner;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import tools.refinery.interpreter.localsearch.planner.cost.IConstraintEvaluationContext;
import tools.refinery.interpreter.matchers.backend.ResultProviderRequestor;
import tools.refinery.interpreter.matchers.context.IQueryBackendContext;
import tools.refinery.interpreter.matchers.context.IQueryResultProviderAccess;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.analysis.QueryAnalyzer;

/**
 * Wraps a PConstraint together with information required for the planner. Currently contains information about the expected binding state of
 * the affected variables also called application condition, and the cost of the enforcement, based on the meta and/or the runtime context.
 *
 * @author Marton Bur
 * @noreference This class is not intended to be referenced by clients.
 */
public class PConstraintInfo implements IConstraintEvaluationContext {

    private PConstraint constraint;
    private Set<PVariable> boundMaskVariables;
    private Set<PVariable> freeMaskVariables;
    private Set<PConstraintInfo> sameWithDifferentBindings;
    private IQueryRuntimeContext runtimeContext;
    private QueryAnalyzer queryAnalyzer;
    private IQueryResultProviderAccess resultProviderAccess;
    private ResultProviderRequestor resultRequestor;

    private Double cost;
    private Function<IConstraintEvaluationContext, Double> costFunction;


    /**
     * Instantiates the wrapper
     * @param constraintfor which the information is added and stored
     * @param boundMaskVariables the bound variables in the operation mask
     * @param freeMaskVariables the free variables in the operation mask
     * @param sameWithDifferentBindings during the planning process, multiple operation adornments are considered for a constraint, so that it
     * is represented by multiple plan infos. This parameter contains all plan infos that are for the same
     * constraint, but with different adornment
     * @param context the query backend context
     */
    public PConstraintInfo(PConstraint constraint, Set<PVariable> boundMaskVariables, Set<PVariable> freeMaskVariables,
        Set<PConstraintInfo> sameWithDifferentBindings,
        IQueryBackendContext context,
        ResultProviderRequestor resultRequestor,
        Function<IConstraintEvaluationContext, Double> costFunction) {
        this.constraint = constraint;
        this.costFunction = costFunction;
        this.boundMaskVariables = new LinkedHashSet<>(boundMaskVariables);
        this.freeMaskVariables = new LinkedHashSet<>(freeMaskVariables);
        this.sameWithDifferentBindings = sameWithDifferentBindings;
        this.resultRequestor = resultRequestor;
        this.runtimeContext = context.getRuntimeContext();
        this.queryAnalyzer = context.getQueryAnalyzer();
        this.resultProviderAccess = context.getResultProviderAccess();

        this.cost = null; // cost will be computed lazily (esp. important for pattern calls)
    }

    @Override
    public IQueryRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    @Override
    public QueryAnalyzer getQueryAnalyzer() {
        return queryAnalyzer;
    }

    @Override
    public PConstraint getConstraint() {
        return constraint;
    }

    @Override
    public Set<PVariable> getFreeVariables() {
        return freeMaskVariables;
    }

    @Override
    public Set<PVariable> getBoundVariables() {
        return boundMaskVariables;
    }

    public Set<PConstraintInfo> getSameWithDifferentBindings() {
        return sameWithDifferentBindings;
    }

    public double getCost() {
        if (cost == null) {
            // Calculate cost of the constraint based on its type
            cost = costFunction.apply(this);
        }
        return cost;
    }

    public PConstraintCategory getCategory(PBody pBody, Set<PVariable> boundVariables) {
        if (!Collections.disjoint(boundVariables, this.freeMaskVariables)) {
            return PConstraintCategory.PAST;
        } else if (!boundVariables.containsAll(this.boundMaskVariables)) {
            return PConstraintCategory.FUTURE;
        } else {
            return PConstraintCategory.PRESENT;
        }
    }

    @Override
    public String toString() {
        return String.format("%s, bound variables: %s, cost: \"%.2f\"", constraint.toString(), boundMaskVariables.toString(), cost);
    }

    /**
     * @deprecated use {@link #resultProviderRequestor()}
     */
    @Override
    @Deprecated
    public IQueryResultProviderAccess resultProviderAccess() {
        return resultProviderAccess;
    }

    @Override
    public ResultProviderRequestor resultProviderRequestor() {
        return resultRequestor;
    }

}
