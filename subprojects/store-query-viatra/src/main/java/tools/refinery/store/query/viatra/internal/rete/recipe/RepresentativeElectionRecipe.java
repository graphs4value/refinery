/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.recipe;

import org.eclipse.viatra.query.runtime.rete.recipes.AlphaRecipe;
import tools.refinery.store.query.literal.Connectivity;

public interface RepresentativeElectionRecipe extends AlphaRecipe {
	Connectivity getConnectivity();

	void setConnectivity(Connectivity connectivity);

	int getArity();
}
