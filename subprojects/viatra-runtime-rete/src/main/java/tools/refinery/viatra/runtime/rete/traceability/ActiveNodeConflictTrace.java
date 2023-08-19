/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.rete.traceability;

import tools.refinery.viatra.runtime.rete.recipes.ReteNodeRecipe;

public class ActiveNodeConflictTrace extends RecipeTraceInfo { // TODO implement PatternTraceInfo
    RecipeTraceInfo inactiveRecipeTrace;		
    public ActiveNodeConflictTrace(ReteNodeRecipe recipe,
            RecipeTraceInfo parentRecipeTrace,
            RecipeTraceInfo inactiveRecipeTrace) {
        super(recipe, parentRecipeTrace);
        this.inactiveRecipeTrace = inactiveRecipeTrace;
    }
    public RecipeTraceInfo getInactiveRecipeTrace() {
        return inactiveRecipeTrace;
    }
}