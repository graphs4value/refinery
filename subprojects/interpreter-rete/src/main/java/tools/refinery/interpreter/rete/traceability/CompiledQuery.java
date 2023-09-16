/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.traceability;

import java.util.Map;

import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;

/**
 * Indicates that recipe expresses the finished match set of a query.
 * @author Bergmann Gabor
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class CompiledQuery extends RecipeTraceInfo implements
        PatternTraceInfo {

    private PQuery query;
    private final Map<PBody, ? extends RecipeTraceInfo> parentRecipeTracesPerBody;

    /**
     * @since 1.6
     */
    public CompiledQuery(ReteNodeRecipe recipe,
                         Map<PBody, ? extends RecipeTraceInfo> parentRecipeTraces,
                         PQuery query) {
        super(recipe, parentRecipeTraces.values());
        parentRecipeTracesPerBody = parentRecipeTraces;
        this.query = query;
    }
    public PQuery getQuery() {
        return query;
    }

    @Override
    public String getPatternName() {
        return query.getFullyQualifiedName();
    }

    /**
     * @since 1.6
     */
    public Map<PBody, ? extends RecipeTraceInfo> getParentRecipeTracesPerBody() {
        return parentRecipeTracesPerBody;
    }

}
