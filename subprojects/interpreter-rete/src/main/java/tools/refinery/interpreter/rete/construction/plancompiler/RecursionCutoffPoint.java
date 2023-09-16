/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.construction.plancompiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration;
import tools.refinery.interpreter.rete.recipes.ProductionRecipe;
import tools.refinery.interpreter.rete.traceability.CompiledQuery;
import tools.refinery.interpreter.rete.traceability.RecipeTraceInfo;

/**
 * In a recursive query structure, query composition references can be cut off so that the remaining structure is DAG.
 * {@link RecursionCutoffPoint} represents one such cut off query composition.
 * When the compilation of the recursive query finishes and the compiled form becomes available,
 *   the {@link RecursionCutoffPoint} has to be signaled to update parent traces and recipes of the recursive call.
 *
 * @author Bergmann Gabor
 * @noreference This class is not intended to be referenced by clients
 *
 */
public class RecursionCutoffPoint {
    final Map<PBody, RecipeTraceInfo> futureTraceMap;
    final CompiledQuery compiledQuery;
    final ProductionRecipe recipe;
    final QueryEvaluationHint hint;

    public RecursionCutoffPoint(PQuery query, QueryEvaluationHint hint, IQueryMetaContext context, boolean deleteAndRederiveEvaluation, TimelyConfiguration timelyEvaluation) {
        super();
        this.hint = hint;
        this.futureTraceMap = new HashMap<>(); // IMPORTANT: the identity of futureTraceMap.values() will not change
        this.compiledQuery = CompilerHelper.makeQueryTrace(query, futureTraceMap, Collections.<ReteNodeRecipe>emptySet(), hint, context, deleteAndRederiveEvaluation, timelyEvaluation);
        this.recipe = (ProductionRecipe)compiledQuery.getRecipe();
        if (!compiledQuery.getParentRecipeTraces().isEmpty()) {
            throw new IllegalArgumentException(String.format("Recursion cut-off point of query %s has trace parents: %s",
                    compiledQuery.getQuery(),
                    prettyPrintParentRecipeTraces(compiledQuery.getParentRecipeTraces())));
        }
        if (!recipe.getParents().isEmpty()) {
            throw new IllegalArgumentException(String.format("Recursion cut-off point of query %s has recipe parents: %s",
                    compiledQuery.getQuery(),
                    prettyPrintParentRecipeTraces(compiledQuery.getParentRecipeTraces())));
        }
    }



    private String prettyPrintParentRecipeTraces(List<RecipeTraceInfo> trace) {
        return trace.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    /**
     * Signals that compilation of the recursive query has terminated, culminating into the given compiled form.
     * The query composition that has been cut off will be connected now.
     */
    public void mend(CompiledQuery finalCompiledForm) {
        futureTraceMap.putAll(finalCompiledForm.getParentRecipeTracesPerBody());
        recipe.getParents().addAll(((ProductionRecipe)finalCompiledForm.getRecipe()).getParents());
    }

    public CompiledQuery getCompiledQuery() {
        return compiledQuery;
    }

    public ProductionRecipe getRecipe() {
        return recipe;
    }



}
