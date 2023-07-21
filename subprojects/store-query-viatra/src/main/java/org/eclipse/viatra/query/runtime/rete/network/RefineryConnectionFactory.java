/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.query.runtime.rete.network;

import org.eclipse.viatra.query.runtime.rete.traceability.RecipeTraceInfo;
import tools.refinery.store.query.viatra.internal.rete.network.RefineryConnectionFactoryExtensions;

/**
 * This class overrides some RETE connection types from {@link ConnectionFactory}.
 * <p>
 * Since {@link ConnectionFactory} is package-private, this class has to be in the
 * {@code org.eclipse.viatra.query.runtime.rete.network} package as well.
 * However, due to JAR signature verification errors, <b>this class cannot be loaded directly</b>
 * and has to be loaded at runtime as a byte array instead.
 */
@SuppressWarnings("unused")
public class RefineryConnectionFactory extends ConnectionFactory {
	private final RefineryConnectionFactoryExtensions extensions;

	public RefineryConnectionFactory(ReteContainer reteContainer) {
		super(reteContainer);
		extensions = new RefineryConnectionFactoryExtensions(reteContainer);
	}

	@Override
	public void connectToParents(RecipeTraceInfo recipeTrace, Node freshNode) {
		if (!extensions.connectToParents(recipeTrace, freshNode)) {
			super.connectToParents(recipeTrace, freshNode);
		}
	}
}
