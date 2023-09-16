/**
 * Copyright (c) 2004-2014 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.interpreter.rete.recipes.helper;

import org.eclipse.emf.common.util.EList;
import tools.refinery.interpreter.rete.recipes.*;

import java.util.Collection;

/**
 * Static helper class for easy construction of recipes.
 *
 * @author Bergmann Gabor
 */
public class RecipesHelper {
    private static final RecipesFactory FACTORY = RecipesFactory.eINSTANCE;

    private RecipesHelper() {/* Utility class constructor */}

    /**
     * @since 2.0
     */
    public static Mask mask(final int sourceArity, final Collection<Integer> sourceIndices) {
        Mask mask = RecipesHelper.FACTORY.createMask();
        mask.setSourceArity(sourceArity);
        mask.getSourceIndices().addAll(sourceIndices);
        return mask;
    }

    public static Mask mask(final int sourceArity, final int... sourceIndices) {
        Mask mask = RecipesHelper.FACTORY.createMask();
        mask.setSourceArity(sourceArity);
        final EList<Integer> maskIndeces = mask.getSourceIndices();
        for (int index : sourceIndices) {
            maskIndeces.add(index);
        }
        return mask;
    }

    public static ProjectionIndexerRecipe projectionIndexerRecipe(final ReteNodeRecipe parent, final Mask mask) {
        ProjectionIndexerRecipe recipe = RecipesHelper.FACTORY.createProjectionIndexerRecipe();
        recipe.setParent(parent);
        recipe.setMask(mask);
        return recipe;
    }

    public static ExpressionDefinition expressionDefinition(final Object evaluator) {
        ExpressionDefinition definition = RecipesHelper.FACTORY.createExpressionDefinition();
        definition.setEvaluator(evaluator);
        return definition;
    }

    public static InputRecipe inputRecipe(final Object inputKey, final String inputKeyID, final int arity) {
        InputRecipe recipe = RecipesHelper.FACTORY.createInputRecipe();
        recipe.setInputKey(inputKey);
        recipe.setKeyArity(arity);
        recipe.setKeyID(inputKeyID);
        recipe.setTraceInfo(inputKeyID);
        return recipe;
    }

    /**
     * Mask can be null in case no tuple reordering or trimming is needed
     */
    public static InputFilterRecipe inputFilterRecipe(final ReteNodeRecipe parent, final Object inputKey,
            final String inputKeyID, final Mask mask) {
        InputFilterRecipe it = RecipesHelper.FACTORY.createInputFilterRecipe();
        it.setParent(parent);
        it.setInputKey(inputKey);
        it.setKeyID(inputKeyID);
        it.setTraceInfo(inputKeyID);
        it.setMask(mask);
        return it;
    }
}
