/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.rete.network;

import org.eclipse.viatra.query.runtime.rete.network.ReteContainer;
import org.eclipse.viatra.query.runtime.rete.network.Supplier;
import org.eclipse.viatra.query.runtime.rete.recipes.ReteNodeRecipe;
import org.eclipse.viatra.query.runtime.rete.traceability.TraceInfo;
import org.jetbrains.annotations.Nullable;
import tools.refinery.store.query.viatra.internal.rete.recipe.RepresentativeElectionRecipe;

public class RefineryNodeFactoryExtensions {
	@Nullable
	public Supplier createNode(ReteContainer reteContainer, ReteNodeRecipe recipe, TraceInfo... traces) {
		var result = instantiateNode(reteContainer, recipe);
		if (result == null) {
			return null;
		}
		for (var traceInfo : traces) {
			result.assignTraceInfo(traceInfo);
		}
		return result;
	}

	@Nullable
	private Supplier instantiateNode(ReteContainer reteContainer, ReteNodeRecipe recipe) {
		if (recipe instanceof RepresentativeElectionRecipe representativeElectionRecipe) {
			return instantiateRepresentativeElectionNode(reteContainer, representativeElectionRecipe);
		}
		return null;
	}

	private Supplier instantiateRepresentativeElectionNode(ReteContainer reteContainer,
														   RepresentativeElectionRecipe recipe) {
		RepresentativeElectionAlgorithm.Factory algorithmFactory = switch (recipe.getConnectivity()) {
			case STRONG -> StronglyConnectedComponentAlgorithm::new;
			case WEAK -> WeaklyConnectedComponentAlgorithm::new;
		};
		return new RepresentativeElectionNode(reteContainer, algorithmFactory);
	}
}
