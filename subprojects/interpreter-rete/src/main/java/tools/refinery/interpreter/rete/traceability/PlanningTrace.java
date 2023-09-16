/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.traceability;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;

/**
 * A trace marker associating a Rete recipe with a query SubPlan.
 *
 * <p> The recipe may be an auxiliary node;
 *   see {@link CompiledSubPlan} if it represents the entire SubPlan instead.
 */
public class PlanningTrace extends RecipeTraceInfo implements PatternTraceInfo {

    protected SubPlan subPlan;
    protected List<PVariable> variablesTuple;
    protected Map<PVariable, Integer> posMapping;

    public PlanningTrace(SubPlan subPlan, List<PVariable> variablesTuple,
            ReteNodeRecipe recipe,
            Collection<? extends RecipeTraceInfo> parentRecipeTraces) {
        super(recipe, parentRecipeTraces);
        this.subPlan = subPlan;
        this.variablesTuple = variablesTuple;

        this.posMapping = new HashMap<PVariable, Integer>();
        for (int i = 0; i < variablesTuple.size(); ++i)
            posMapping.put(variablesTuple.get(i), i);
    }

    public PlanningTrace(SubPlan subPlan, List<PVariable> variablesTuple,
            ReteNodeRecipe recipe,
            RecipeTraceInfo... parentRecipeTraces) {
        this(subPlan, variablesTuple, recipe, Arrays.asList(parentRecipeTraces));
    }

    public SubPlan getSubPlan() {
        return subPlan;
    }

    @Override
    public String getPatternName() {
        return subPlan.getBody().getPattern().getFullyQualifiedName();
    }

    public List<PVariable> getVariablesTuple() {
        return variablesTuple;
    }

    public Map<PVariable, Integer> getPosMapping() {
        return posMapping;
    }

    /**
     * Returns a new clone that reinterprets the same compiled form
     *  as the compiled form of a (potentially different) subPlan.
     * Useful e.g. if child plan turns out to be a no-op, or when promoting a {@link PlanningTrace} to {@link CompiledSubPlan}.
     */
    public CompiledSubPlan cloneFor(SubPlan newSubPlan) {
        return new CompiledSubPlan(newSubPlan,
                getVariablesTuple(),
                getRecipe(),
                getParentRecipeTracesForCloning());
    }

}
