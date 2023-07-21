/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.network;

import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.rete.network.Node;
import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.Supplier;
import org.eclipse.viatra.query.runtime.rete.remote.Address;
import org.eclipse.viatra.query.runtime.rete.traceability.RecipeTraceInfo;
import tools.refinery.store.query.viatra.internal.rete.recipe.RepresentativeElectionRecipe;

import java.util.ArrayList;

public class RefineryConnectionFactoryExtensions {
	private final ReteContainer reteContainer;

	public RefineryConnectionFactoryExtensions(ReteContainer reteContainer) {
		this.reteContainer = reteContainer;
	}

	public boolean connectToParents(RecipeTraceInfo recipeTrace, Node freshNode) {
		var recipe = recipeTrace.getRecipe();
		if (recipe instanceof RepresentativeElectionRecipe representativeElectionRecipe) {
			connectToParents(representativeElectionRecipe, (RepresentativeElectionNode) freshNode);
			return true;
		}
		return false;
	}

	private void connectToParents(RepresentativeElectionRecipe recipe, RepresentativeElectionNode freshNode) {
		var parentRecipe = recipe.getParent();
		// Apparently VIATRA ensures that this cast is safe, see
		// {@link org.eclipse.viatra.query.runtime.rete.network.ConnectionFactory.connectToParent}.
		@SuppressWarnings("unchecked")
		var parentAddress = (Address<? extends Supplier>) reteContainer.getNetwork()
				.getExistingNodeByRecipe(parentRecipe);
		var parentSupplier = reteContainer.getProvisioner().asSupplier(parentAddress);
		var tuples = new ArrayList<Tuple>();
		parentSupplier.pullInto(tuples, true);
		freshNode.reinitializeWith(tuples);
		reteContainer.connect(parentSupplier, freshNode);
	}
}
