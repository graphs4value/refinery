/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.traceability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.recipes.ReteNodeRecipe;

/**
 * A trace marker that indicates the recipe for which the node was built.
 * @author Bergmann Gabor
 */
public class RecipeTraceInfo implements TraceInfo {
    public ReteNodeRecipe getRecipe() {return recipe;}
    /**
     * For cloning in case of recursion cut-off points, use {@link #getParentRecipeTracesForCloning()} instead.
     * @return an unmodifiable view on parent traces, to be constructed before this node (or alongside, in case of recursion)
     */
    public List<RecipeTraceInfo> getParentRecipeTraces() {return Collections.unmodifiableList(new ArrayList<>(parentRecipeTraces));}
    /**
     * Directly return the underlying collection so that changes to it will be transparent. Use only for recursion-tolerant cloning.
     * @noreference This method is not intended to be referenced by clients.
     */
    public Collection<? extends RecipeTraceInfo> getParentRecipeTracesForCloning() {return parentRecipeTraces;}
    @Override
    public Node getNode() {return node;}

    private Node node;
    ReteNodeRecipe recipe;
    ReteNodeRecipe shadowedRecipe;
    Collection<? extends RecipeTraceInfo> parentRecipeTraces;


    public RecipeTraceInfo(ReteNodeRecipe recipe, Collection<? extends RecipeTraceInfo> parentRecipeTraces) {
        super();
        this.recipe = recipe;
        this.parentRecipeTraces = parentRecipeTraces; //ParentTraceList.from(parentRecipeTraces);
    }
    public RecipeTraceInfo(ReteNodeRecipe recipe, RecipeTraceInfo... parentRecipeTraces) {
        this(recipe, Arrays.asList(parentRecipeTraces));
    }

    @Override
    public boolean propagateToIndexerParent() {return false;}
    @Override
    public boolean propagateFromIndexerToSupplierParent() {return false;}
    @Override
    public boolean propagateFromStandardNodeToSupplierParent() {return false;}
    @Override
    public boolean propagateToProductionNodeParentAlso() {return false;}
    @Override
    public void assignNode(Node node) {this.node = node;}

    /**
     * @param knownRecipe a known recipe that is equivalent to the current recipe
     */
    public void shadowWithEquivalentRecipe(ReteNodeRecipe knownRecipe) {
        this.shadowedRecipe = this.recipe;
        this.recipe = knownRecipe;
    }

    /**
     * Get original recipe shadowed by an equivalent
     */
    public ReteNodeRecipe getShadowedRecipe() {
        return shadowedRecipe;
    }


}
