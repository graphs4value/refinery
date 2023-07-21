/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.recipe;

import org.eclipse.emf.ecore.*;
import org.eclipse.viatra.query.runtime.rete.recipes.RecipesPackage;

// Naming and index computation follows EMF conventions.
@SuppressWarnings({"squid:S100", "squid:S115", "PointlessArithmeticExpression"})
public interface RefineryRecipesPackage extends EPackage {
	String eNAME = "refineryReteRecipes";

	String eNS_URI = "https://refinery.tools/emf/2021/RefineryReteRecipes";

	String eNS_PREFIX = "refineryReteRecipes";

	RefineryRecipesPackage eINSTANCE = RefineryRecipesPackageImpl.init();

	int REPRESENTATIVE_ELECTION_RECIPE = 0;

	int REPRESENTATIVE_ELECTION_RECIPE__CONNECTIVITY = RecipesPackage.ALPHA_RECIPE_FEATURE_COUNT + 0;

	int REPRESENTATIVE_ELECTION_RECIPE_FEATURE_COUNT = RecipesPackage.ALPHA_RECIPE_FEATURE_COUNT + 1;

	int REPRESENTATIVE_ELECTION_RECIPE__GET_ARITY = RecipesPackage.ALPHA_RECIPE_OPERATION_COUNT + 0;

	int REPRESENTATIVE_ELECTION_RECIPE_OPERATION_COUNT = RecipesPackage.ALPHA_RECIPE_OPERATION_COUNT + 1;

	int CONNECTIVITY = 1;

	EClass getRepresentativeElectionRecipe();

	EAttribute getRepresentativeElectionRecipe_Connectivity();

	EOperation getRepresentativeElectionRecipe_GetArity();

	EDataType getConnectivity();
}
