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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.util.Preconditions;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;

/**
 * A trace marker associating a Rete recipe with a query SubPlan.
 *
 * <p> The Rete node represented by the recipe is equivalent to the SubPlan.
 * <p> Invariant: each variable has at most one index associated with it in the tuple, i.e. no duplicates.
 */
public class CompiledSubPlan extends PlanningTrace {

    public CompiledSubPlan(SubPlan subPlan, List<PVariable> variablesTuple,
                           ReteNodeRecipe recipe,
                           Collection<? extends RecipeTraceInfo> parentRecipeTraces) {
        super(subPlan, variablesTuple, recipe, parentRecipeTraces);

        // Make sure that each variable occurs only once
        Set<PVariable> variablesSet = new HashSet<PVariable>(variablesTuple);
        Preconditions.checkState(variablesSet.size() == variablesTuple.size(),
                () -> String.format(
                        "Illegal column duplication (%s) while the query plan %s was compiled into a Rete Recipe %s",
                        variablesTuple.stream().map(PVariable::getName).collect(Collectors.joining(",")),
                        subPlan.toShortString(), recipe));
    }

    public CompiledSubPlan(SubPlan subPlan, List<PVariable> variablesTuple,
            ReteNodeRecipe recipe,
            RecipeTraceInfo... parentRecipeTraces) {
        this(subPlan, variablesTuple, recipe, Arrays.asList(parentRecipeTraces));
    }

}
