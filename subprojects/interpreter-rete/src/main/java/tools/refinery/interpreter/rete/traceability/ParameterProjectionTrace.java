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

import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;

/**
 * The recipe projects the finished results of a {@link PBody} onto the list of parameters.
 * @author Bergmann Gabor
 *
 */
public class ParameterProjectionTrace extends RecipeTraceInfo implements PatternTraceInfo {

    public ParameterProjectionTrace(PBody body, ReteNodeRecipe recipe,
            RecipeTraceInfo... parentRecipeTraces) {
        this(body, recipe, Arrays.asList(parentRecipeTraces));
    }

    public ParameterProjectionTrace(PBody body, ReteNodeRecipe recipe,
            Collection<? extends RecipeTraceInfo> parentRecipeTraces) {
        super(recipe, parentRecipeTraces);
        this.body = body;
    }

    PBody body;

    @Override
    public String getPatternName() {
        return body.getPattern().getFullyQualifiedName();
    }

}
